import { Component, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';

/**
 * Composant racine : en-tête (liens selon l'état de connexion) et zone d'affichage des pages.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('pmt-frontend');

  constructor(
    readonly authService: AuthService,
    private readonly router: Router,
  ) {}

  /** Déconnecte l'utilisateur et le renvoie vers la page de connexion. */
  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }
}
