/** Identifiants envoyés à `POST /api/auth/login`. */
export interface LoginRequest {
  email: string;
  password: string;
}

/** Données envoyées à `POST /api/auth/register`. */
export interface RegisterRequest extends LoginRequest {
  name: string;
}

/**
 * Utilisateur connecté, tel que renvoyé par le backend ; son `id` sert de `userId` dans les autres appels.
 */
export interface AuthResponse {
  id: number;
  name: string;
  email: string;
}
