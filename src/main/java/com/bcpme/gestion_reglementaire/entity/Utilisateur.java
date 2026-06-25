package com.bcpme.gestion_reglementaire.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    @Column(nullable = false)
    private Boolean actif = true;

    @Column(nullable = false)
    private Integer tentativesConnexion = 0;

    @ManyToOne
    private Role role;

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public Integer getTentativesConnexion() {
        return tentativesConnexion;
    }

    public void setTentativesConnexion(Integer tentativesConnexion) {
        this.tentativesConnexion = tentativesConnexion;
    }
}