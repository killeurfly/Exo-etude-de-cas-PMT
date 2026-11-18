import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateProjectRequest, Project } from '../models/project';

@Injectable({
  providedIn: 'root',
})
export class ProjectService {
  private readonly apiUrl = 'http://localhost:8080/api/projects';

  constructor(private readonly http: HttpClient) {}

  getProjects(userId: number): Observable<Project[]> {
    return this.http.get<Project[]>(this.apiUrl, {
      params: new HttpParams().set('userId', userId),
    });
  }

  createProject(userId: number, request: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.apiUrl, request, {
      params: new HttpParams().set('userId', userId),
    });
  }
}

export function getProjectErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 400) {
      return 'Le nom, la description et la date de début sont obligatoires.';
    }
    if (error.status === 404) {
      return 'Utilisateur introuvable. Reconnecte-toi.';
    }
  }

  return fallback;
}