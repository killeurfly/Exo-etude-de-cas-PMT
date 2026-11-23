import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateProjectRequest, Project } from '../models/project';

/**
 * Appels à l'API des projets (`/api/projects`).
 *
 * L'utilisateur connecté est transmis en paramètre `userId` : le backend s'en sert pour vérifier ses droits.
 */
@Injectable({
  providedIn: 'root',
})
export class ProjectService {
  private readonly apiUrl = 'http://localhost:8080/api/projects';

  constructor(private readonly http: HttpClient) {}

  /** Projets dont l'utilisateur est membre actif, du plus récent au plus ancien. */
  getProjects(userId: number): Observable<Project[]> {
    return this.http.get<Project[]>(this.apiUrl, {
      params: new HttpParams().set('userId', userId),
    });
  }

  /** Un projet ; erreur 403 si l'utilisateur n'en est pas membre. */
  getProject(projectId: number, userId: number): Observable<Project> {
    return this.http.get<Project>(`${this.apiUrl}/${projectId}`, {
      params: new HttpParams().set('userId', userId),
    });
  }

  /** Crée un projet ; son créateur en devient administrateur. */
  createProject(userId: number, request: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.apiUrl, request, {
      params: new HttpParams().set('userId', userId),
    });
  }
}

/**
 * Transforme une erreur de l'API en message lisible selon son code HTTP (400, 403, 404), sinon `fallback`.
 * Utilisée aussi pour le chargement des membres, des tâches et de l'historique.
 */
export function getProjectErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 400) {
      return 'Le nom, la description et la date de début sont obligatoires.';
    }
    if (error.status === 403) {
      return 'Tu ne fais pas partie de ce projet.';
    }
    if (error.status === 404) {
      return 'Utilisateur ou projet introuvable. Reconnecte-toi.';
    }
  }

  return fallback;
}