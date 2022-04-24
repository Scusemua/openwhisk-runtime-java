/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.openwhisk.runtime.java.action;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Proxy {
    private final HttpServer server;

    private JarLoader loader = null;

    private final Lock initLock = new ReentrantLock();

    public Proxy(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), -1);

        this.server.createContext("/init", new InitHandler());
        this.server.createContext("/run", new RunHandler());

        String concurrencyProperty = System.getenv("__OW_ALLOW_CONCURRENT");
        // System.out.println("__OW_ALLOW_CONCURRENT = " + concurrencyProperty);
        boolean concurrencyEnabled = Boolean.parseBoolean(concurrencyProperty);

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                10,             // Core size.
                25,             // Max size.
                10 * 60,        // Idle timeout.
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(30)
        );
        executor.allowCoreThreadTimeOut(true);

        if (concurrencyEnabled) {
            // System.out.println("Action-level concurrency is ENABLED.");
            // this.server.setExecutor(Executors.newCachedThreadPool()); // Multi-threaded executor.
            this.server.setExecutor(executor);
        } else {
            // System.out.println("Action-level concurrency is DISABLED.");
            this.server.setExecutor(null); // Default executor.
        }

        System.setSecurityManager(new WhiskSecurityManager());
    }

    public void start() {
        // System.out.println("Starting the proxy's HTTP server now...");
        server.start();
    }

    private static void writeResponse(HttpExchange t, int code, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        System.out.println("Writing " + bytes.length + " bytes back to client...");
        t.sendResponseHeaders(code, bytes.length);
        OutputStream os = t.getResponseBody();
        os.write(bytes);
        os.close();
    }

    /**
     * Write an HTTP response using information provided by the result of user-code execution.
     * @param t Used to get an output stream to write the response to.
     * @param result The result of executing the user's code.
     */
    private static void writeResponse(HttpExchange t, JsonObject result) throws IOException {
        String content = result.toString();

        System.out.println("Response content: " + result);

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        int statusCode = result.get("statusCode").getAsInt();
        t.sendResponseHeaders(statusCode, bytes.length);

        OutputStream os = t.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void writeError(HttpExchange t, String errorMessage) throws IOException {
        JsonObject message = new JsonObject();
        message.addProperty("error", errorMessage);
        writeResponse(t, 502, message.toString());
    }

    private static void writeLogMarkers() {
        System.out.println("XXX_THE_END_OF_A_WHISK_ACTIVATION_XXX");
        // System.err.println("XXX_THE_END_OF_A_WHISK_ACTIVATION_XXX");

        System.out.println("Total Heap Memory: " + (Runtime.getRuntime().totalMemory() / 1000000.0) +
                " MB\nAvailable Heap Memory: " + (Runtime.getRuntime().freeMemory() / 1000000.0) +
                " MB.");

        System.out.flush();
        System.err.flush();
    }

    private class InitHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            System.setProperty("sun.io.serialization.extendedDebugInfo", "true");

            if (loader != null) {
                String errorMessage = "Cannot initialize the action more than once.";
                System.err.println(errorMessage);
                Proxy.writeError(t, errorMessage);
                return;
            }

            System.out.println("RECEIVED INITIALIZATION!");

            initLock.lock();
            try {
                InputStream is = t.getRequestBody();
                JsonParser parser = new JsonParser();
                JsonElement ie = parser.parse(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
                JsonObject inputObject = ie.getAsJsonObject();

                if (inputObject.has("value")) {
                    JsonObject message = inputObject.getAsJsonObject("value");
                    if (message.has("main") && message.has("code")) {
                        String mainClass = message.getAsJsonPrimitive("main").getAsString();
                        String base64Jar = message.getAsJsonPrimitive("code").getAsString();

                        // FIXME: this is obviously not very useful. The idea is that we
                        //  will implement/use a streaming parser for the incoming JSON object so that we
                        //  can stream the contents of the jar straight to a file.
                        InputStream jarIs = new ByteArrayInputStream(base64Jar.getBytes(StandardCharsets.UTF_8));

                        // Save the bytes to a file.
                        Path jarPath = JarLoader.saveBase64EncodedFile(jarIs);

                        // Start up the custom classloader. This also checks that the
                        // main method exists.
                        loader = new JarLoader(jarPath, mainClass);

                        Proxy.writeResponse(t, 200, "OK");
                        return;
                    }
                }

                Proxy.writeError(t, "Missing main/no code to execute.");
            } catch (Exception e) {
                e.printStackTrace(System.err);
                writeLogMarkers();
                Proxy.writeError(t, "An error has occurred (see logs for details): " + e);
            } finally {
                initLock.unlock();
            }
        }
    }

    private class RunHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            System.setSecurityManager(new WhiskSecurityManager());
            if (loader == null) {
                Proxy.writeError(t, "Cannot invoke an uninitialized action.");
                return;
            }

            System.out.println("RECEIVED INVOCATION. Heap Memory In-Use: " + (Runtime.getRuntime().totalMemory() / 1000000.0) +
                    " MB\nHeap Memory Free/Available: " + (Runtime.getRuntime().freeMemory() / 1000000.0) +
                    " MB.");

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            
            // SecurityManager sm = System.getSecurityManager();

            try {
                InputStream is = t.getRequestBody();
                JsonParser parser = new JsonParser();
                JsonObject body = parser.parse(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))).getAsJsonObject();
                JsonObject inputObject = body.getAsJsonObject("value");

                HashMap<String, String> env = new HashMap<String, String>();
                Set<Map.Entry<String, JsonElement>> entrySet = body.entrySet();
                for(Map.Entry<String, JsonElement> entry : entrySet){
                    try {
                        if(!entry.getKey().equalsIgnoreCase("value"))
                            env.put(String.format("__OW_%s", entry.getKey().toUpperCase()), entry.getValue().getAsString());
                    } catch (Exception e) {}
                }

                // =-=-= Commented out bc this is just the IP of the invoker k8s pod =-=-=
                // Add the client's IP address as part of the input.
                // inputObject.addProperty("client_remote_address", t.getRemoteAddress().getHostName());

                Thread.currentThread().setContextClassLoader(loader);

                // User code starts running here.
                JsonObject output = loader.invokeMain(inputObject, env);
                // User code finished running here.

                if (output == null) {
                    throw new NullPointerException("The action returned null");
                }

                int statusCode;

                if (output.has("statusCode"))
                    statusCode = output.get("statusCode").getAsInt();
                else
                    statusCode = 200;

                System.out.println("Writing response with status code " + statusCode + " to user now...");
                // System.out.println("Action output: " + output);
                long writeRespStart = System.nanoTime();
                Proxy.writeResponse(t, statusCode, output.toString());
                long writeRespEnd = System.nanoTime();
                double writeRespDuration = (writeRespEnd - writeRespStart) / 1000000.0;
                System.out.println("Write response to user in " + writeRespDuration + " milliseconds.");
            } catch (InvocationTargetException ite) {
                // These are exceptions from the action, wrapped in ite because
                // of reflection
                Throwable underlying = ite.getCause();
                underlying.printStackTrace(System.err);
                Proxy.writeError(t,
                        "An error has occurred while invoking the action (see logs for details): " + underlying);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                Proxy.writeError(t, "An error has occurred (see logs for details): " + e);
            } finally {
                writeLogMarkers();
                // System.setSecurityManager(sm);
                Thread.currentThread().setContextClassLoader(cl);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("sun.io.serialization.extendedDebugInfo", "true");

        Proxy proxy = new Proxy(8080);
        proxy.start();
    }
}
