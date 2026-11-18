import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateTaskRequest, Task, UpdateTaskRequest } from '../models/task';

@Injectable({
  providedIn: 'root',
})
export class TaskService {
  private readonly apiUrl = 'http://localhost:8080/api/projects';

  constructor(private readonly http: HttpClient) {}

  getTasks(projectId: number, userId: number): Observable<Task[]> {
    return this.http.get<Task[]>(this.tasksUrl(projectId), {
      params: new HttpParams().set('userId', userId),
    });
  }

  createTask(projectId: number, userId: number, request: CreateTaskRequest): Observable<Task> {
    return this.http.post<Task>(this.tasksUrl(projectId), request, {
      params: new HttpParams().set('userId', userId),
    });
  }

  updateTask(projectId: number, taskId: number, userId: number, request: UpdateTaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.tasksUrl(projectId)}/${taskId}`, request, {
      params: new HttpParams().set('userId', userId),
    });
  }

  private tasksUrl(projectId: number): string {
    return `${this.apiUrl}/${projectId}/tasks`;
  }
}

export function getTaskErrorMessage(error: unknown, action: 'create' | 'update' = 'create'): string {
  const verb = action === 'create' ? 'créer' : 'modifier';
  if (error instanceof HttpErrorResponse) {
    if (error.status === 400) {
      return 'Le nom et la description de la tâche sont obligatoires.';
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
