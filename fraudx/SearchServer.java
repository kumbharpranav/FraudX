import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class SearchServer {
    private static final String SEARCH_API_KEY = "";
    private static final String CX = "";
    private static final String GEMINI_API_KEY = "";
    private static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";
    private static final String[] SEARCH_API_KEYS = {""};
    private static int currentKeyIndex = 0;
    private static final Object keyLock = new Object();

    private static String getNextApiKey() {
        synchronized (keyLock) {
            String key = SEARCH_API_KEYS[currentKeyIndex];
            currentKeyIndex = (currentKeyIndex + 1) % SEARCH_API_KEYS.length;
            return key;
        }
    }

    private static String executeSearchWithFailover(String encodedQuery) throws IOException {
        String response = "";
        boolean searchSuccessful = false;
        Exception lastException = null;
        
        for (int i = 0; i < SEARCH_API_KEYS.length && !searchSuccessful; i++) {
            String currentKey = getNextApiKey();
            String searchUrl = "https://www.googleapis.com/customsearch/v1?key=" + currentKey
                            + "&cx=" + CX + "&q=" + encodedQuery;
            
            try {
                URL url = new URL(searchUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder responseBuilder = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        responseBuilder.append(inputLine);
                    }
                    in.close();
                    response = responseBuilder.toString();
                    searchSuccessful = true;
                } else if (responseCode == 429) {
                    continue;
                } else {
                    InputStream errorStream = conn.getErrorStream();
                    if (errorStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder errorBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorBuilder.append(line);
                        }
                        lastException = new IOException("HTTP " + responseCode + ": " + errorBuilder.toString());
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                lastException = e;
            }
        }
        
        if (!searchSuccessful && lastException != null) {
            throw new IOException("All API keys failed. Last error: " + lastException.getMessage());
        }
        
        return response;
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8083), 0);
        server.createContext("/", new SearchHandler());
        server.createContext("/gemini", new GeminiHandler());
        server.createContext("/utf8", new Utf8HttpHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started");
    }
    
    static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String responseHtml = "";
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                String queryParam = exchange.getRequestURI().getQuery();
                String query = "";
                if (queryParam != null) {
                    String[] params = queryParam.split("&");
                    for (String param : params) {
                        if (param.startsWith("query=")) {
                            query = param.substring("query=".length());
                            break;
                        }
                    }
                }
                
                if (query == null || query.trim().isEmpty()) {
                    responseHtml = "<html><head><title>Times of India Search</title></head><body>" +
                                   "<h1>Search Times of India</h1>" +
                                   "<form method='GET' action='/' >" +
                                   "Search Query: <input type='text' name='query' size='40' />" +
                                   "<input type='submit' value='Search' />" +
                                   "</form>" +
                                   "</body></html>";
                } else {
                    query = URLDecoder.decode(query, "UTF-8");
                    String encodedQuery = URLEncoder.encode(query, "UTF-8");
                    String apiUrl = "https://www.googleapis.com/customsearch/v1?key=" + SEARCH_API_KEY
                                    + "&cx=" + CX + "&q=" + encodedQuery;
                    
                    String jsonResponse = "";
                    try {
                        URL url = new URL(apiUrl);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        String inputLine;
                        StringBuilder responseBuilder = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            responseBuilder.append(inputLine);
                        }
                        in.close();
                        conn.disconnect();
                        jsonResponse = responseBuilder.toString();
                    } catch (Exception e) {
                        jsonResponse = "{\"error\": \"" + e.getMessage() + "\"}";
                    }
                    
                    responseHtml = "<html><head><title>Search Results</title>" +
                                   "<style>" +
                                   "body {font-family: Arial, sans-serif;}" +
                                   ".result {margin-bottom:20px; padding-bottom:10px; border-bottom:1px solid #ccc;}" +
                                   "a {text-decoration: none; color: #1a0dab;}" +
                                   "</style>" +
                                   "</head><body>" +
                                   "<h1>Search Results for \"" + query + "\"</h1>";
                    
                    try {
                        JSONObject jsonObj = new JSONObject(jsonResponse);
                        if (jsonObj.has("items")) {
                            JSONArray items = jsonObj.getJSONArray("items");
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                String title = item.optString("title", "No Title");
                                String snippet = item.optString("snippet", "No snippet available");
                                String link = item.optString("link", "#");
                                responseHtml += "<div class='result'>" +
                                                "<h2><a href='" + link + "' target='_blank'>" + title + "</a></h2>" +
                                                "<p>" + snippet + "</p>" +
                                                "<p><a href='" + link + "' target='_blank'>View Full Article</a></p>" +
                                                "</div>";
                            }
                        } else {
                            responseHtml += "<p>No results found.</p>";
                        }
                    } catch (Exception e) {
                        responseHtml += "<p>Error parsing JSON: " + e.getMessage() + "</p>";
                    }
                    
                    responseHtml += "<p><a href='/'>New Search</a></p>" +
                                    "</body></html>";
                }
            } else {
                responseHtml = "<html><body><h1>Unsupported HTTP Method</h1></body></html>";
            }
            
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            byte[] bytes = responseHtml.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
    
    static class GeminiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String messageText = "";
            
            if ("GET".equalsIgnoreCase(method)) {
                String queryParam = exchange.getRequestURI().getQuery();
                if (queryParam != null) {
                    String[] params = queryParam.split("&");
                    for (String param : params) {
                        if (param.startsWith("messageText=")) {
                            messageText = URLDecoder.decode(param.substring("messageText=".length()), "UTF-8");
                            break;
                        }
                    }
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                String requestBody = readStream(exchange.getRequestBody());
                JSONObject requestJson;
                try {
                    requestJson = new JSONObject(requestBody);
                } catch (Exception e) {
                    String errorResponse = "{\"error\": \"Invalid JSON in request body\"}";
                    exchange.sendResponseHeaders(400, errorResponse.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(errorResponse.getBytes());
                    os.close();
                    return;
                }
                messageText = requestJson.optString("messageText", "");
            } else {
                String response = "Unsupported HTTP method";
                exchange.sendResponseHeaders(405, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }
            
            String encodedQuery = URLEncoder.encode(messageText, "UTF-8");
            String item = "";
            try {
                String searchResponse = executeSearchWithFailover(encodedQuery);
                JSONObject jsonResponse = new JSONObject(searchResponse);
                if (jsonResponse.has("items")) {
                    JSONArray itemsArray = jsonResponse.getJSONArray("items");
                    StringBuilder summaryBuilder = new StringBuilder();
                    int resultsToUse = Math.min(itemsArray.length(), 2);
                    for (int i = 0; i < resultsToUse; i++) {
                        JSONObject result = itemsArray.getJSONObject(i);
                        String title = result.optString("title", "No Title");
                        String snippet = result.optString("snippet", "No snippet available");
                        summaryBuilder.append("Result ").append(i+1).append(": ")
                                    .append(title).append(" - ").append(snippet).append("\n");
                    }
                    item = summaryBuilder.toString();
                } else {
                    item = "No search results found for the query.";
                }
            } catch (Exception e) {
                item = "Search error: " + e.getMessage();
            }
            
            String prePrompt = ", Based STRICTLY on the following search results and ONLY these results, answer with 'REAL ' or 'FAKE ' followed by a maximum of 4 sentences explaining why. Do not refer to 'the provided info' or 'results' in your answer and use space after real or fake word. Search results: ";
            String finalPrompt = messageText + prePrompt + item;
            
            JSONObject geminiRequest = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObj = new JSONObject();
            contentObj.put("role", "user");
            JSONArray partsArray = new JSONArray();
            JSONObject partObj = new JSONObject();
            partObj.put("text", finalPrompt);
            partsArray.put(partObj);
            contentObj.put("parts", partsArray);
            contentsArray.put(contentObj);
            geminiRequest.put("contents", contentsArray);
            
            String geminiResponse = "";
            try {
                URL url = new URL(GEMINI_ENDPOINT);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(geminiRequest.toString());
                dos.flush();
                dos.close();
                
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                String inputLine;
                StringBuilder responseBuilder = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseBuilder.append(inputLine);
                }
                in.close();
                conn.disconnect();
                geminiResponse = responseBuilder.toString();
            } catch (Exception e) {
                geminiResponse = "{\"error\": \"" + e.getMessage() + "\"}";
            }
            
            String finalGeminiResult = "";
            try {
                JSONObject geminiJson = new JSONObject(geminiResponse);
                if (geminiJson.has("candidates")) {
                    JSONArray candidates = geminiJson.getJSONArray("candidates");
                    if (candidates.length() > 0) {
                        JSONObject candidate = candidates.getJSONObject(0);
                        if (candidate.has("content")) {
                            JSONObject content = candidate.getJSONObject("content");
                            if (content.has("parts")) {
                                JSONArray parts = content.getJSONArray("parts");
                                if (parts.length() > 0) {
                                    finalGeminiResult = parts.getJSONObject(0).optString("text", "No response");
                                }
                            }
                        }
                    }
                }
                if (finalGeminiResult.isEmpty()) {
                    finalGeminiResult = geminiResponse;
                }
            } catch (Exception ex) {
                finalGeminiResult = "{\"error\": \"Failed to parse Gemini response: " + ex.getMessage() + "\"}";
            }
            
            JSONObject output = new JSONObject();
            output.put("result", finalGeminiResult);
            
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = output.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
        
        private String readStream(InputStream is) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    
    static class Utf8HttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "<html><head><meta charset='UTF-8'></head><body>" +
                              "<h1>Welcome to the SearchServer</h1>" +
                              "<p>This page supports UTF-8 and emojis 😊!</p>" +
                              "</body></html>";
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}
