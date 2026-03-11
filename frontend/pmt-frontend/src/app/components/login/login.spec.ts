// src/app/components/login/login.spec.ts

import { LoginComponent, LoginRequest } from './login';
import { AuthService } from '../../services/auth.service';
import { of, throwError } from 'rxjs';
import { ChangeDetectorRef } from '@angular/core';

// Fake AuthService pour les tests
class FakeAuthService {
  login(user: LoginRequest) {
    if (user.email === 'test@example.com' && user.password === '1234') {
      return of({ message: 'Connexion OK' });
    } else {
      return throwError({ error: { message: 'Email ou mot de passe invalide' } });
    }
  }
}

// Fake ChangeDetectorRef pour tests
class FakeChangeDetectorRef {
  detectChanges() {
    // rien à faire pour le test
  }
}

// Création des instances pour le test
const fakeAuth = new FakeAuthService() as unknown as AuthService;
const fakeCD = new FakeChangeDetectorRef() as unknown as ChangeDetectorRef;

// Création du composant LoginComponent
const loginComponent = new LoginComponent(fakeAuth, fakeCD);

// Test 1 : connexion réussie
loginComponent.user = { email: 'test@example.com', password: '1234' };
loginComponent.login(new Event('submit'));
console.log('Message succès:', loginComponent.message);

// Test 2 : connexion échouée
loginComponent.user = { email: 'fail@example.com', password: '0000' };
loginComponent.login(new Event('submit'));
console.log('Message erreur:', loginComponent.message);