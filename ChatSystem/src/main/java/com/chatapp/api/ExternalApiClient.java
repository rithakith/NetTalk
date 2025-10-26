package com.chatapp.api;

import com.chatapp.common.Message;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * MEMBER 5 CONTRIBUTION: External API Integration
 * 
 * Network Programming Concept: HTTP Client using URL/URLConnection
 * 
 * This class demonstrates how to make HTTP requests to external APIs
 * using Java's built-in URL and URLConnection classes. It fetches
 * dynamic data from public APIs when specific commands are typed in chat.
 * 
 * Key Concepts Demonstrated:
 * - HTTP client operations with URL/URLConnection
 * - Making GET requests to external services
 * - Parsing HTTP responses
 * - Integration of external data into chat application
 * 
 * Supported Commands:
 * - /weather <city> - Get weather information
 * - /joke - Get random joke
 * - /quote - Get random quote
 */
public class ExternalApiClient {
    
    // Free public APIs (no authentication required)
    private static final String WEATHER_API = "https://wttr.in/";
    private static final String JOKE_API = "https://official-joke-api.appspot.com/random_joke";
    private static final String QUOTE_API = "https://api.quotable.io/random";
    
    /**
     * Process API request based on message content
     */
    public static Message processApiRequest(Message request) {
        String content = request.getContent().trim();
        String response;
        
        System.out.println("[ExternalApiClient] Processing API request: " + content);
        
        try {
            if (content.startsWith("/weather ")) {
                String city = content.substring(9).trim();
                response = getWeather(city);
            } else if (content.equalsIgnoreCase("/joke")) {
                response = getRandomJoke();
            } else if (content.equalsIgnoreCase("/quote")) {
                response = getRandomQuote();
            } else if (content.equalsIgnoreCase("/help")) {
                response = getHelpText();
            } else {
                response = "Unknown API command. Type /help for available commands.";
            }
        } catch (Exception e) {
            response = "Error fetching data: " + e.getMessage();
            System.err.println("[ExternalApiClient] API error: " + e.getMessage());
        }
        
        return new Message(Message.MessageType.API_RESPONSE, "API Bot", response);
    }
    
    /**
     * Get weather information for a city using wttr.in API
     * Example: https://wttr.in/London?format=3
     */
    private static String getWeather(String city) throws IOException {
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8.toString());
        String urlString = WEATHER_API + encodedCity + "?format=3";
        
        System.out.println("[ExternalApiClient] Fetching weather for: " + city);
        
        String response = makeHttpGetRequest(urlString);
        return "🌤️ Weather for " + city + ":\n" + response;
    }
    
    /**
     * Get a random joke from Official Joke API
     */
    private static String getRandomJoke() throws IOException {
        System.out.println("[ExternalApiClient] Fetching random joke");
        
        String response = makeHttpGetRequest(JOKE_API);
        
        // Simple JSON parsing (for production, use a proper JSON library)
        String setup = extractJsonValue(response, "setup");
        String punchline = extractJsonValue(response, "punchline");
        
        return "😄 Random Joke:\n" + setup + "\n" + punchline;
    }
    
    /**
     * Get a random quote from Quotable API
     */
    private static String getRandomQuote() throws IOException {
        System.out.println("[ExternalApiClient] Fetching random quote");
        
        String response = makeHttpGetRequest(QUOTE_API);
        
        // Simple JSON parsing
        String content = extractJsonValue(response, "content");
        String author = extractJsonValue(response, "author");
        
        return "💭 Quote of the moment:\n\"" + content + "\"\n- " + author;
    }
    
    /**
     * Make HTTP GET request using URL and URLConnection
     * This is the core network programming demonstration
     */
    private static String makeHttpGetRequest(String urlString) throws IOException {
        // Create URL object
        URL url = new URL(urlString);
        
        // Open connection - this is the key URLConnection usage
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // Configure connection
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 seconds
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "ChatApp/1.0");
            
            // Get response code
            int responseCode = connection.getResponseCode();
            System.out.println("[ExternalApiClient] HTTP Response Code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return response.toString();
            } else {
                throw new IOException("HTTP request failed with code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Simple JSON value extractor (basic implementation)
     * For production, use Jackson or Gson
     */
    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return "";
        
        startIndex += searchKey.length();
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) return "";
        
        return json.substring(startIndex, endIndex);
    }
    
    /**
     * Get help text for available API commands
     */
    private static String getHelpText() {
        return "🤖 Available API Commands:\n" +
               "/weather <city> - Get current weather for a city\n" +
               "/joke - Get a random joke\n" +
               "/quote - Get an inspirational quote\n" +
               "/help - Show this help message";
    }
}
