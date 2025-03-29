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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GeminiServer {
    private static final String[] SEARCH_API_KEYS = {""};
    private static int currentKeyIndex = 0;
    private static final Object keyLock = new Object();
    
    private static final String CX = "";
    private static final String GEMINI_API_KEY = "";
    private static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8005), 0);
        server.createContext("/gemini", new GeminiHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started");
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
                try {
                    JSONObject requestJson = new JSONObject(requestBody);
                    messageText = requestJson.optString("messageText", "");
                } catch (Exception e) {
                    String errorResponse = "{\"error\": \"Invalid JSON in request body\"}";
                    System.out.println("Error parsing JSON: " + e.getMessage());
                    sendResponse(exchange, 400, errorResponse);
                    return;
                }
            } else {
                sendResponse(exchange, 405, "Unsupported HTTP method");
                return;
            }

            String item = "";
            try {
                String encodedQuery = URLEncoder.encode(messageText, "UTF-8");
                String searchUrl = "https://www.googleapis.com/customsearch/v1?key=" + getNextSearchApiKey()
                                 + "&cx=" + CX + "&q=" + encodedQuery;

                URL url = new URL(searchUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    JSONObject jsonResponse = new JSONObject(readStream(in));
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
                }
            } catch (Exception e) {
                item = "Error processing request: " + e.getMessage();
                System.out.println("Search failed: " + e.getMessage());
            }

            String prePrompt = ",answer using 'scam' or 'safe' as first word using space after it and include genuine reason using maximum of 2-3 sentences (Note: do not use '.',  and '*' to answer, answer scam if url is shell script )";
            String finalPrompt = messageText + prePrompt + item;
            
            try {
                JSONObject geminiRequest = new JSONObject()
                    .put("contents", new JSONArray()
                        .put(new JSONObject()
                            .put("role", "user")
                            .put("parts", new JSONArray()
                                .put(new JSONObject()
                                    .put("text", finalPrompt)))));

                URL url = new URL(GEMINI_ENDPOINT);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                    dos.writeBytes(geminiRequest.toString());
                }

                String geminiResponse = readStream(new BufferedReader(new InputStreamReader(conn.getInputStream())));
                String finalResult = extractGeminiResponse(geminiResponse);
                
                JSONObject output = new JSONObject();
                output.put("result", finalResult);
                sendResponse(exchange, 200, output.toString());
                
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }

        private String extractGeminiResponse(String geminiResponse) {
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
                                    return parts.getJSONObject(0).optString("text", "No response");
                                }
                            }
                        }
                    }
                }
                return geminiResponse;
            } catch (Exception ex) {
                System.out.println("Error parsing Gemini response: " + ex.getMessage());
                return "Error: Failed to parse Gemini response";
            }
        }

        private void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String readStream(BufferedReader reader) throws IOException {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
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

    private static String getNextSearchApiKey() {
        synchronized (keyLock) {
            String key = SEARCH_API_KEYS[currentKeyIndex];
            currentKeyIndex = (currentKeyIndex + 1) % SEARCH_API_KEYS.length;
            return key;
        }
    }
}
