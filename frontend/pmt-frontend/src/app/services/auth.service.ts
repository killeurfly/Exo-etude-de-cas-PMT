import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly apiUrl = 'http://localhost:8080/api/auth';

  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request);
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request);
  }
}

export function getAuthErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    if (typeof error.error === 'object' && error.error !== null) {
      const body = error.error as { detail?: unknown; message?: unknown };
      if (typeof body.message === 'string') {
        return body.message;
      }
      if (typeof body.detail === 'string') {
        return body.detail;
      }
    }

    if (error.status === 409) {
      return 'Cette adresse e-mail est déjà utilisée.';
    }
    if (error.status === 401) {
      return 'Adresse e-mail ou mot de passe incorrect.';
    }
  }

  return fallback;
}