package com.chatapp.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserAuthService {
    // Simulated user database (you can later connect this to a real DB)
    private static final Map<String, String> users = new ConcurrentHashMap<>();

    static {
        // Default users
        users.put("admin", "admin123");
        users.put("guest", "guest123");
    }

    public static boolean register(String username, String password) {
        if (users.containsKey(username)) return false;
        users.put(username, password);
        return true;
    }

    public static boolean authenticate(String username, String password) {
        return users.containsKey(username) && users.get(username).equals(password);
    }
}
