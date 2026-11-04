// src/app/components/register/register.ts
import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { RegisterRequest } from '../../models/auth';
import { AuthService, getAuthErrorMessage } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html',
})
export class RegisterComponent {
  user: RegisterRequest = { email: '', password: '', name: '' };
  readonly message = signal('');
  readonly loading = signal(false);

  constructor(private readonly authService: AuthService) {}

  register(): void {
    this.message.set('');
    this.loading.set(true);

    this.authService.register(this.user).subscribe({
      next: () => {
        this.loading.set(false);
        this.message.set('Inscription réussie. Vous pouvez vous connecter.');
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.message.set(getAuthErrorMessage(error, 'Erreur lors de l’inscription.'));
      },
    });
  }
}