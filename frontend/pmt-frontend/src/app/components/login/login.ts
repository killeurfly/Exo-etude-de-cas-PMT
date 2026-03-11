import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ChangeDetectorRef } from '@angular/core';

export interface LoginRequest {
  email: string;
  password: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html'
})
export class LoginComponent {
  user: LoginRequest = { email: '', password: '' };
  message: string = '';

  constructor(
    private authService: AuthService,
    private cd: ChangeDetectorRef // ← Inject ici
  ) {}

  login(event: Event) {
    event.preventDefault();
    this.message = '';

    this.authService.login(this.user).subscribe({
      next: (res: any) => {
        this.message = res.message || 'Connexion réussie !';
        this.cd.detectChanges(); // ← Force l’update du template
      },
      error: (err: any) => {
        this.message = err.error?.message || 'Email ou mot de passe invalide';
        this.cd.detectChanges(); // ← Force l’update du template
      }
    });
  }
}