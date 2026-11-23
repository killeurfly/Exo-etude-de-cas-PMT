import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LoginRequest } from '../../models/auth';
import { AuthService, getAuthErrorMessage } from '../../services/auth.service';

/** Page de connexion ; redirige vers le tableau de bord une fois connecté. */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
})
export class LoginComponent {
  user: LoginRequest = { email: '', password: '' };
  readonly message = signal('');
  readonly loading = signal(false);

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {}

  /** Envoie les identifiants et affiche l'erreur éventuelle. */
  login(): void {
    this.message.set('');
    this.loading.set(true);

    this.authService.login(this.user).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigate(['/dashboard']);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.message.set(getAuthErrorMessage(error, 'E-mail ou mot de passe incorrect.'));
      },
    });
  }
}