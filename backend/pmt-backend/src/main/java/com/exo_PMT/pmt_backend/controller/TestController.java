package com.exo_PMT.pmt_backend.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple vérification que le serveur tourne ({@code GET /hello}), masquée de la documentation de l'API.
 */
@Hidden
@RestController
public class TestController {

    @GetMapping("/hello")
    public String hello() {
        return "PMT backend is running!";
    }
}