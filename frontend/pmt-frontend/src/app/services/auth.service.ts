import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth';

/**
 * Connexion, inscription et mémorisation de l'utilisateur connecté.
 *
 * L'utilisateur est gardé dans un signal et dans le `localStorage`, pour rester connecté après
 * un rechargement de la page. Il n'y a pas de jeton : la sécurisation n'est pas demandée par l'énoncé.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private static readonly CURRENT_USER_KEY = 'pmt.currentUser';
  private readonly apiUrl = 'http://localhost:8080/api/auth';
  private readonly currentUserState = signal<AuthResponse | null>(this.readCurrentUser());
  readonly currentUser = this.currentUserState.asReadonly();

  constructor(private readonly http: HttpClient) {}

  /** Connecte l'utilisateur et le mémorise si les identifiants sont acceptés. */
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap((user) => {
        localStorage.setItem(AuthService.CURRENT_USER_KEY, JSON.stringify(user));
        this.currentUserState.set(user);
      }),
    );
  }

  /** Crée un compte. L'utilisateur doit ensuite se connecter. */
  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request);
  }

  /** Oublie l'utilisateur connecté. */
  logout(): void {
    localStorage.removeItem(AuthService.CURRENT_USER_KEY);
    this.currentUserState.set(null);
  }

  /**
   * Relit l'utilisateur mémorisé au démarrage. Une valeur illisible ou incomplète
   * (modifiée à la main, ancienne version) est effacée plutôt que d'être utilisée.
   */
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

  /** Vérifie qu'une valeur relue du `localStorage` a bien la forme d'un utilisateur. */
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

/**
 * Transforme une erreur de connexion ou d'inscription en message lisible : message du backend s'il y en a un,
 * sinon texte selon le code HTTP (401, 409), sinon `fallback`.
 */
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