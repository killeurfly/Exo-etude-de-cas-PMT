import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthResponse } from '../models/auth';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  const storageKey = 'pmt.currentUser';
  const user: AuthResponse = { id: 7, name: 'Test User', email: 'test@example.test' };
  let service: AuthService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    localStorage.removeItem(storageKey);
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
    localStorage.removeItem(storageKey);
  });

  it('should store the user after a successful login', () => {
    service = TestBed.inject(AuthService);
    service.login({ email: user.email, password: 'pass123' }).subscribe();
    httpTestingController.expectOne('http://localhost:8080/api/auth/login').flush(user);

    expect(service.currentUser()).toEqual(user);
    expect(localStorage.getItem(storageKey)).toBe(JSON.stringify(user));
  });

  it('should restore a valid stored user', () => {
    localStorage.setItem(storageKey, JSON.stringify(user));
    service = TestBed.inject(AuthService);

    expect(service.currentUser()).toEqual(user);
  });

  it('should remove invalid stored user data', () => {
    localStorage.setItem(storageKey, '{invalid');
    service = TestBed.inject(AuthService);

    expect(service.currentUser()).toBeNull();
    expect(localStorage.getItem(storageKey)).toBeNull();
  });
});
