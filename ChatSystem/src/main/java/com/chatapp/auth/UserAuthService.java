package com.chatapp.auth;

import com.chatapp.util.PasswordUtil;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Secure user authentication service using PasswordUtil
 * Stores passwords as "hashedPassword:salt" for security
 * PRIMARY AUTHENTICATION SERVICE FOR THE APPLICATION
 */
public class UserAuthService {
    // Secure user storage: username -> "hashedPassword:salt"
    private static final Map<String, String> users = new ConcurrentHashMap<>();
    private static final Map<String, String> userRoles = new ConcurrentHashMap<>();

    static {
        // Initialize with secure default users
        initializeDefaultUsers();
    }

    /**
     * Initialize default users with secure password storage
     */
    private static void initializeDefaultUsers() {
        // Admin user
        String adminSalt = PasswordUtil.generateSalt();
        String adminHash = PasswordUtil.hashPassword("admin123", adminSalt);
        users.put("admin", adminHash + ":" + adminSalt);
        userRoles.put("admin", "admin");

        // Guest user
        String guestSalt = PasswordUtil.generateSalt();
        String guestHash = PasswordUtil.hashPassword("guest123", guestSalt);
        users.put("guest", guestHash + ":" + guestSalt);
        userRoles.put("guest", "user");

        System.out.println("[UserAuthService] Default users initialized with secure storage");
    }

    /**
     * Register a new user with secure password storage
     */
    public static boolean register(String username, String plaintextPassword) {
        return register(username, plaintextPassword, "user");
    }

    /**
     * Register a new user with specific role
     */
    public static boolean register(String username, String plaintextPassword, String role) {
        if (users.containsKey(username)) {
            System.out.println("[UserAuthService] Registration failed - user exists: " + username);
            return false;
        }

        if (username == null || username.trim().isEmpty() || plaintextPassword == null || plaintextPassword.trim().isEmpty()) {
            System.out.println("[UserAuthService] Registration failed - invalid username/password");
            return false;
        }

        // Generate salt and hash password
        String salt = PasswordUtil.generateSalt();
        String hashedPassword = PasswordUtil.hashPassword(plaintextPassword, salt);

        // Store as "hashedPassword:salt"
        users.put(username, hashedPassword + ":" + salt);
        userRoles.put(username, role);
        System.out.println("[UserAuthService] User registered securely: " + username + " (role: " + role + ")");
        return true;
    }

    /**
     * Authenticate user with secure password verification
     */
    public static boolean authenticate(String username, String plaintextPassword) {
        if (!users.containsKey(username)) {
            System.out.println("[UserAuthService] Authentication failed - user not found: " + username);
            return false;
        }

        String storedValue = users.get(username);
        String[] parts = storedValue.split(":");

        if (parts.length != 2) {
            System.out.println("[UserAuthService] Invalid stored credentials format for: " + username);
            return false;
        }

        String storedHash = parts[0];
        String salt = parts[1];
        String attemptHash = PasswordUtil.hashPassword(plaintextPassword, salt);

        boolean success = storedHash.equals(attemptHash);
        System.out.println("[UserAuthService] Authentication " + (success ? "successful" : "failed") + " for: " + username);
        return success;
    }

    /**
     * Check if user exists
     */
    public static boolean userExists(String username) {
        return users.containsKey(username);
    }

    /**
     * Get user role
     */
    public static String getRole(String username) {
        return userRoles.getOrDefault(username, "user");
    }

    /**
     * Get all users (for debugging/admin purposes)
     */
    public static Map<String, String> getUsers() {
        return new ConcurrentHashMap<>(users);
    }

    /**
     * Get user count
     */
    public static int getUserCount() {
        return users.size();
    }

    /**
     * Remove user (admin function)
     */
    public static boolean removeUser(String username) {
        if (users.containsKey(username)) {
            users.remove(username);
            userRoles.remove(username);
            System.out.println("[UserAuthService] User removed: " + username);
            return true;
        }
        return false;
    }

    /**
     * Validate credentials and return role if valid
     */
    public static String validateAndGetRole(String username, String password) {
        if (authenticate(username, password)) {
            return getRole(username);
        }
        return null;
    }
}