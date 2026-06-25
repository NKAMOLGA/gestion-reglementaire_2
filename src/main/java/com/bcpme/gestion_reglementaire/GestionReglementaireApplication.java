package com.bcpme.gestion_reglementaire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestionReglementaireApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionReglementaireApplication.class, args);
    }
}