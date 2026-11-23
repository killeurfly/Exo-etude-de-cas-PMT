// src/app/components/register/register.ts
import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { RegisterRequest } from '../../models/auth';
import { AuthService, getAuthErrorMessage } from '../../services/auth.service';

/** Page d'inscription ; une fois le compte créé, un message invite à se connecter. */
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html',
})
export class RegisterComponent {
  user: RegisterRequest = { email: '', password: '', name: '' };
  readonly message = signal('');
  readonly messageIsError = signal(false);
  readonly loading = signal(false);

  constructor(private readonly authService: AuthService) {}

  /** Envoie le formulaire d'inscription et affiche l'erreur éventuelle. */
  register(): void {
    this.message.set('');
    this.loading.set(true);

    this.authService.register(this.user).subscribe({
      next: () => {
        this.loading.set(false);
        this.showMessage('Inscription réussie. Vous pouvez vous connecter.');
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.showMessage(getAuthErrorMessage(error, 'Erreur lors de l’inscription.'), true);
      },
    });
  }

  /** Affiche un message ; une erreur est mise en évidence en rouge. */
  private showMessage(text: string, isError = false): void {
    this.message.set(text);
    this.messageIsError.set(isError);
  }
}