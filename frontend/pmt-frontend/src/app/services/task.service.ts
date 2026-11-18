import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateTaskRequest, Task } from '../models/task';

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

  private tasksUrl(projectId: number): string {
    return `${this.apiUrl}/${projectId}/tasks`;
  }
}

export function getTaskErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 400) {
      return 'Le nom et la description de la tâche sont obligatoires.';
    }
    if (error.status === 403) {
      return 'Ton rôle ne permet pas de créer une tâche dans ce projet.';
    }
  }

  return 'Impossible de créer la tâche.';
}
