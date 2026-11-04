import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthResponse } from '../../models/auth';
import { AuthService } from '../../services/auth.service';
import { RegisterComponent } from './register';

describe('RegisterComponent', () => {
  const response: AuthResponse = { id: 1, name: 'Test User', email: 'test@example.test' };
  const authService = { register: vi.fn() };

  beforeEach(async () => {
    authService.register.mockReset();

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    }).compileComponents();
  });

  it('should register and display a success message', () => {
    authService.register.mockReturnValue(of(response));
    const component = TestBed.createComponent(RegisterComponent).componentInstance;
    component.user = { name: 'Test User', email: 'test@example.test', password: 'pass123' };

    component.register();

    expect(authService.register).toHaveBeenCalledWith(component.user);
    expect(component.message()).toBe('Inscription réussie. Vous pouvez vous connecter.');
    expect(component.loading()).toBe(false);
  });

  it('should display an API error when registration fails', () => {
    authService.register.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 409,
        error: { timestamp: '2026-11-04T11:11:19.064Z', status: 409, error: 'Conflict', path: '/api/auth/register' },
      })),
    );
    const component = TestBed.createComponent(RegisterComponent).componentInstance;

    component.register();

    expect(component.message()).toBe('Cette adresse e-mail est déjà utilisée.');
    expect(component.loading()).toBe(false);
  });
});
