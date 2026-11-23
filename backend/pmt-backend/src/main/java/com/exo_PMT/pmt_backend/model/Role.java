package com.exo_PMT.pmt_backend.model;

/** Rôle d'un membre dans un projet ; il détermine ses droits. */
public enum Role {
    /** Tous les droits : inviter, gérer les rôles, créer, assigner et modifier des tâches. */
    ADMIN,
    /** Créer, assigner et modifier des tâches. */
    MEMBER,
    /** Lecture seule : tâches, tableau de bord et historique. */
    OBSERVER
}
