package com.cart.ecom_proj.dto;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.*;

public class LoginRequest {
    @NotBlank @Size(min=3,max=40) @NotBlank @Size(min=3,max=40) private String username;
    @NotBlank @Size(min=8,max=72) @NotBlank @Size(min=8,max=72) private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
