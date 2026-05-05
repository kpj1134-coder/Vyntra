package com.vyntra.dto;

public class AuthResponse {

    private String token;
    private String name;
    private String email;
    private String userId;

    public AuthResponse() {}

    public AuthResponse(String token, String name, String email, String userId) {
        this.token = token;
        this.name = name;
        this.email = email;
        this.userId = userId;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
