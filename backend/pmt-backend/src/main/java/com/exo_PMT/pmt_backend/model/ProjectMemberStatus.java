package com.exo_PMT.pmt_backend.model;

/** État d'une adhésion à un projet. */
public enum ProjectMemberStatus {
    /** Invitation envoyée à un e-mail sans compte ; ne donne pas encore accès au projet. */
    INVITED,
    /** Membre inscrit, qui a accès au projet selon son rôle. */
    ACTIVE
}
