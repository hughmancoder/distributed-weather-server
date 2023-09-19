package com;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.*;
import java.util.Timer;

import com.google.gson.JsonParseException;
import com.models.WeatherData;
import com.utility.LamportClock;
import com.utility.JsonUtils;

public class AggregationServer {
    public static final String TEMP_STORAGE_PATH = "../../resources/temp_storage.json";

    private static final ReentrantLock lock = new ReentrantLock();
    private static LamportClock lamportClock = new LamportClock();
    private static HashMap<String, WeatherData> weatherDataMap = new HashMap<>();

    // private static PriorityBlockingQueue<WeatherData> weatherDataQueue = new
    // PriorityBlockingQueue<>();
    // private static HashMap<String, Long> lastActiveMap = new HashMap<>();

    /*
     * public static void main(String[] args) {
     * int port = 4567;
     * 
     * if (args.length > 0) {
     * try {
     * port = Integer.parseInt(args[0]);
     * } catch (NumberFormatException e) {
     * e.printStackTrace();
     * System.out.println("Invalid port number. Using default port 4567.");
     * }
     * }
     * System.out.println("Running aggregation server on port " + port + "..");
     * 
     * // TODO: Load initial data from content server, if you need to
     * try {
     * // Uncomment this once you have the loadDataFromFile method ready.
     * // loadDataFromFile(DATA_FILE_PATH);
     * // put to weather map
     * } catch (Exception e) {
     * System.out.println("Failed to load data from file");
     * e.printStackTrace();
     * }
     * 
     * try {
     * HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
     * server.createContext("/weather", new DataHandler());
     * server.setExecutor(Executors.newFixedThreadPool(10));
     * server.start();
     * 
     * Timer timer = new Timer();
     * timer.schedule(new TimerTask() {
     * 
     * @Override
     * public void run() {
     * removeOldEntries();
     * }
     * }, 30000, 30000);
     * 
     * } catch (IOException e) {
     * e.printStackTrace();
     * System.out.println("Failed to start HTTP server.");
     * }
     * }
     */

    private static HttpServer server;
    private static Timer timer;
    private static boolean isRunning = false;

    public static void main(String[] args) {
        int port = 4567;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                System.out.println("Invalid port number. Using default port 4567");
            }
        }

        start(port);
    }

    public static void start(int port) {
        System.out.println("Running aggregation server on port " + port + "..");
        try {

            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/weather", new DataHandler());
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    removeOldEntries();
                }
            }, 30000, 30000);

            isRunning = true;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to start HTTP server.");
        }
    }

    public static void stop() {
        if (isRunning) {
            isRunning = false;
            if (timer != null) {
                timer.cancel();
            }
            if (server != null) {
                server.stop(0);
            }
        }
    }

    public static void clearWeatherDataMap() {
        lock.lock();
        try {
            weatherDataMap.clear();
        } finally {
            lock.unlock();
        }
    }

    public static HashMap<String, WeatherData> getWeatherDataMap() {
        return weatherDataMap;
    }

    public static HashMap<String, WeatherData> putToWeatherDataMap(WeatherData weatherData)
            throws IllegalArgumentException {
        lock.lock();
        try {
            String id = weatherData.getId();

            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("WeatherData object must have a valid id");
            }

            weatherDataMap.put(id, weatherData);

            return new HashMap<>(weatherDataMap); // returns a shallow copy
        } finally {
            lock.unlock();
        }
    }

    public static void WeatherDataMapToFile() {
        lock.lock();
        try {
            Path path = Paths.get(TEMP_STORAGE_PATH);
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            String json = JsonUtils.hashMapToJson(weatherDataMap);
            Files.write(path, json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to save data to file.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public static void FileToWeatherDataMap(String filePath) throws IOException {
        lock.lock();
        try {
            if (Files.exists(Paths.get(filePath))) {
                String json = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
                HashMap<String, WeatherData> dataMap = JsonUtils.jsonToHashMap(json);
                if (dataMap != null) {
                    weatherDataMap.putAll(dataMap);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("Failed to load data from file", e);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    static class DataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestMethod = httpExchange.getRequestMethod();

            if ("GET".equals(requestMethod)) {
                GETRequest(httpExchange);
            } else if ("PUT".equals(requestMethod)) {
                PUTRequest(httpExchange);
            } else {
                httpExchange.sendResponseHeaders(400, 0); // Bad Request
                httpExchange.close();
            }
        }

        private void GETRequest(HttpExchange httpExchange) throws IOException {
            lock.lock();
            try {
                URI requestURI = httpExchange.getRequestURI();
                Map<String, String> queryParameters = parseQueryParameters(requestURI.getQuery());

                String stationId = queryParameters.get("station");
                String json;

                if (stationId != null) {
                    // Send data for the specific station
                    WeatherData weatherData = weatherDataMap.get(stationId);
                    if (weatherData != null) {
                        json = JsonUtils.toJson(weatherData);
                    } else {
                        json = "{ \"error\": \"No data available for stationId: " + stationId + "\" }";
                        httpExchange.sendResponseHeaders(404, json.length());
                    }
                } else {
                    // Send all data
                    json = JsonUtils.hashMapToJson(weatherDataMap);
                }

                byte[] response = json.getBytes();
                httpExchange.sendResponseHeaders(200, response.length);
                httpExchange.getResponseBody().write(response);

            } finally {
                lock.unlock();
                httpExchange.close();
            }
        }

        // Helper method to parse query parameters
        private Map<String, String> parseQueryParameters(String query) throws UnsupportedEncodingException {
            Map<String, String> result = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] entry = param.split("=");
                    if (entry.length > 1) {
                        result.put(URLDecoder.decode(entry[0], "UTF-8"), URLDecoder.decode(entry[1], "UTF-8"));
                    }
                }
            }
            return result;
        }

        private void PUTRequest(HttpExchange httpExchange) throws IOException {
            lock.lock();
            try {
                // Read the request body
                InputStream is = httpExchange.getRequestBody();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                String requestBody = sb.toString();
                WeatherData weatherData = JsonUtils.fromJson(requestBody);

                // Validate the received WeatherData object
                if (weatherData == null || weatherData.getId() == null || weatherData.getId().isEmpty()) {
                    httpExchange.sendResponseHeaders(400, 0); // Bad Request
                    return;
                }
                // Call the static PUTRequest to update data and get the status code
                int statusCode = AggregationServer.PUTRequest(weatherData);

                // Send the response headers
                httpExchange.sendResponseHeaders(statusCode, 0);

            } catch (JsonParseException e) {
                e.printStackTrace();
                httpExchange.sendResponseHeaders(400, 0); // Bad Request
            } catch (Exception e) {
                e.printStackTrace();
                httpExchange.sendResponseHeaders(500, 0); // Internal Server Error
            } finally {
                lock.unlock();
                httpExchange.close();
            }
        }
    }

    public static int PUTRequest(WeatherData data) {
        int statusCode;
        if (weatherDataMap.isEmpty()) {
            statusCode = 201; // Created
        } else {
            statusCode = 200; // OK
        }

        lock.lock();
        try {
            // TODO: update other map
            weatherDataMap.put(data.getId(), data);
        } catch (Exception e) {
            e.printStackTrace();
            statusCode = 500; // Internal Server Error
        } finally {
            lock.unlock();
        }

        return statusCode;
    }

    private static void removeOldEntries() {
        lock.lock();
        try {
            long currentTime = lamportClock.getTime();
            // TODO: get string key and remove from weatherDataMap;
            // lastActiveMap.entrySet().removeIf(entryx -> currentTime - entry.getValue() >
            // 30);
        } finally {
            lock.unlock();
        }
    }
}
