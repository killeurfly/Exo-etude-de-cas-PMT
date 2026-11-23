package com.exo_PMT.pmt_backend.model;

/** Avancement d'une tâche ; c'est le critère des colonnes du tableau de bord. */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    /** Terminée : la date de fin de la tâche est alors renseignée. */
    DONE
}
