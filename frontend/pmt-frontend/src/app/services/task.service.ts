import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateTaskRequest, Task, TaskHistoryEntry, UpdateTaskRequest } from '../models/task';

/**
 * Appels à l'API des tâches d'un projet (`/api/projects/{projectId}/tasks`).
 *
 * L'utilisateur connecté est transmis en paramètre `userId` : le backend s'en sert pour vérifier ses droits.
 */
@Injectable({
  providedIn: 'root',
})
export class TaskService {
  private readonly apiUrl = 'http://localhost:8080/api/projects';

  constructor(private readonly http: HttpClient) {}

  /** Tâches du projet, de la plus récente à la plus ancienne. */
  getTasks(projectId: number, userId: number): Observable<Task[]> {
    return this.http.get<Task[]>(this.tasksUrl(projectId), {
      params: new HttpParams().set('userId', userId),
    });
  }

  /** Crée une tâche (rôles ADMIN et MEMBER). */
  createTask(projectId: number, userId: number, request: CreateTaskRequest): Observable<Task> {
    return this.http.post<Task>(this.tasksUrl(projectId), request, {
      params: new HttpParams().set('userId', userId),
    });
  }

  /** Remplace les informations d'une tâche (rôles ADMIN et MEMBER). */
  updateTask(projectId: number, taskId: number, userId: number, request: UpdateTaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.tasksUrl(projectId)}/${taskId}`, request, {
      params: new HttpParams().set('userId', userId),
    });
  }

  /** Une tâche du projet. */
  getTask(projectId: number, taskId: number, userId: number): Observable<Task> {
    return this.http.get<Task>(`${this.tasksUrl(projectId)}/${taskId}`, {
      params: new HttpParams().set('userId', userId),
    });
  }

  /** Historique des modifications d'une tâche, de la plus récente à la plus ancienne. */
  getHistory(projectId: number, taskId: number, userId: number): Observable<TaskHistoryEntry[]> {
    return this.http.get<TaskHistoryEntry[]>(`${this.tasksUrl(projectId)}/${taskId}/history`, {
      params: new HttpParams().set('userId', userId),
    });
  }

  private tasksUrl(projectId: number): string {
    return `${this.apiUrl}/${projectId}/tasks`;
  }
}

/**
 * Message affiché quand la création ou la modification d'une tâche est refusée (400, 403, 404).
 */
export function getTaskErrorMessage(error: unknown, action: 'create' | 'update' = 'create'): string {
  const verb = action === 'create' ? 'créer' : 'modifier';
  if (error instanceof HttpErrorResponse) {
    if (error.status === 400) {
      return 'Le nom et la description sont obligatoires, et la tâche ne peut être assignée qu’à un membre du projet.';
    }
    if (error.status === 403) {
      return `Ton rôle ne permet pas de ${verb} une tâche dans ce projet.`;
    }
    if (error.status === 404) {
      return 'Cette tâche n’existe plus dans ce projet.';
    }
  }

  return `Impossible de ${verb} la tâche.`;
}
