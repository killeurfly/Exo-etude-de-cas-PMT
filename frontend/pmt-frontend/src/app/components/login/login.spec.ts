import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthResponse } from '../../models/auth';
import { AuthService } from '../../services/auth.service';
import { LoginComponent } from './login';

describe('LoginComponent', () => {
  const response: AuthResponse = { id: 1, name: 'Test User', email: 'test@example.test' };
  const authService = { login: vi.fn() };
  let router: Router;

  beforeEach(async () => {
    authService.login.mockReset();

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('should log in and navigate to the dashboard', () => {
    authService.login.mockReturnValue(of(response));
    const component = TestBed.createComponent(LoginComponent).componentInstance;
    component.user = { email: 'test@example.test', password: 'pass123' };

    component.login();

    expect(authService.login).toHaveBeenCalledWith(component.user);
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(component.loading()).toBe(false);
  });

  it('should display an API error when login fails', () => {
    authService.login.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 401,
        error: { timestamp: '2026-11-04T11:11:19.064Z', status: 401, error: 'Unauthorized', path: '/api/auth/login' },
      })),
    );
    const component = TestBed.createComponent(LoginComponent).componentInstance;

    component.login();

    expect(component.message()).toBe('Adresse e-mail ou mot de passe incorrect.');
    expect(component.loading()).toBe(false);
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
