// src/app/components/register/register.ts
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './register.html',
})
export class RegisterComponent {
  user: User = { email: '', password: '', name: '' };
  message: string = '';

  constructor(private authService: AuthService) {}

  register() {
    this.authService.register(this.user).subscribe({
      next: (res) => {
        this.message = 'Inscription réussie !';
        console.log(res);
      },
      error: (err) => {
        this.message = 'Erreur lors de l’inscription';
        console.error(err);
      },
    });
  }
}