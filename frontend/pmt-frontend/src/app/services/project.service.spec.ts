import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { getProjectErrorMessage, ProjectService } from './project.service';

describe('ProjectService', () => {
  const projectsUrl = 'http://localhost:8080/api/projects';
  let service: ProjectService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProjectService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should call the project endpoints with the user id', () => {
    const request = { name: 'PMT', description: 'Gestion de projets', startDate: '2026-12-01' };

    service.getProjects(1).subscribe();
    service.getProject(10, 1).subscribe();
    service.createProject(1, request).subscribe();

    httpTestingController.expectOne({ method: 'GET', url: `${projectsUrl}?userId=1` });
    httpTestingController.expectOne({ method: 'GET', url: `${projectsUrl}/10?userId=1` });
    const create = httpTestingController.expectOne({ method: 'POST', url: `${projectsUrl}?userId=1` });
    expect(create.request.body).toEqual(request);
  });

  it('should translate project errors and fall back to the given message', () => {
    const fallback = 'Impossible de charger le projet.';

    expect(getProjectErrorMessage(new HttpErrorResponse({ status: 400 }), fallback)).toContain('obligatoires');
    expect(getProjectErrorMessage(new HttpErrorResponse({ status: 403 }), fallback)).toBe('Tu ne fais pas partie de ce projet.');
    expect(getProjectErrorMessage(new HttpErrorResponse({ status: 404 }), fallback)).toContain('introuvable');
    expect(getProjectErrorMessage(new HttpErrorResponse({ status: 500 }), fallback)).toBe(fallback);
    expect(getProjectErrorMessage(new Error('réseau'), fallback)).toBe(fallback);
  });
});
