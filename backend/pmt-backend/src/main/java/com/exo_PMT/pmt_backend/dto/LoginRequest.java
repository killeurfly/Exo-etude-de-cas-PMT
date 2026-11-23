package com.exo_PMT.pmt_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Identifiants de connexion.")
public class LoginRequest {
    @Schema(description = "Adresse e-mail du compte.", example = "alice@example.test")
    private String email;
    @Schema(description = "Mot de passe.", example = "alice123")
    private String password;

    public LoginRequest() {}

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}