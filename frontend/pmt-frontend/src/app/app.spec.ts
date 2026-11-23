import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { AuthResponse } from './models/auth';
import { AuthService } from './services/auth.service';

describe('App', () => {
  const user: AuthResponse = { id: 1, name: 'Test User', email: 'test@example.test' };
  const currentUser = signal<AuthResponse | null>(null);
  const authService = {
    currentUser,
    logout: vi.fn(() => currentUser.set(null)),
  };

  beforeEach(async () => {
    currentUser.set(null);
    authService.logout.mockClear();

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the application title', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Project Management Tool');
  });

  it('should show login and register links when nobody is logged in', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('nav')?.textContent).toContain('Login');
    expect(compiled.querySelector('nav button')).toBeNull();
  });

  it('should log out and go back to the login page', async () => {
    currentUser.set(user);
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('nav')?.textContent).toContain('Test User');

    compiled.querySelector<HTMLButtonElement>('nav button')?.click();
    await fixture.whenStable();

    expect(authService.logout).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/login']);
    expect(compiled.querySelector('nav')?.textContent).toContain('Login');
  });
});
