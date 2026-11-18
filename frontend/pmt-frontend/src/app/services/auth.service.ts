import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private static readonly CURRENT_USER_KEY = 'pmt.currentUser';
  private readonly apiUrl = 'http://localhost:8080/api/auth';
  private readonly currentUserState = signal<AuthResponse | null>(this.readCurrentUser());
  readonly currentUser = this.currentUserState.asReadonly();

  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap((user) => {
        localStorage.setItem(AuthService.CURRENT_USER_KEY, JSON.stringify(user));
        this.currentUserState.set(user);
      }),
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request);
  }

  private readCurrentUser(): AuthResponse | null {
    const storedUser = localStorage.getItem(AuthService.CURRENT_USER_KEY);
    if (storedUser === null) {
      return null;
    }

    try {
      const user: unknown = JSON.parse(storedUser);
      if (this.isAuthResponse(user)) {
        return user;
      }
    } catch {
      localStorage.removeItem(AuthService.CURRENT_USER_KEY);
      return null;
    }

    localStorage.removeItem(AuthService.CURRENT_USER_KEY);
    return null;
  }

  private isAuthResponse(value: unknown): value is AuthResponse {
    return typeof value === 'object'
      && value !== null
      && 'id' in value
      && typeof value.id === 'number'
      && 'name' in value
      && typeof value.name === 'string'
      && 'email' in value
      && typeof value.email === 'string';
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