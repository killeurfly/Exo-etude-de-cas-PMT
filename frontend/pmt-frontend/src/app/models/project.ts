/** Projet tel que renvoyé par l'API. */
export interface Project {
  id: number;
  name: string;
  description: string;
  startDate: string;
  createdByUserId: number;
}

/** Données de création d'un projet (date au format AAAA-MM-JJ). */
export interface CreateProjectRequest {
  name: string;
  description: string;
  startDate: string;
}