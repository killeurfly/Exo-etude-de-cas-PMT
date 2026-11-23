package com.exo_PMT.pmt_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Point d'entrée de l'API REST de PMT (Project Management Tool). */
@SpringBootApplication
public class PmtBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(PmtBackendApplication.class, args);
    }
}