import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { AuthResponse } from '../../models/auth';
import { Project } from '../../models/project';
import { AuthService } from '../../services/auth.service';
import { ProjectService } from '../../services/project.service';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;
  const user: AuthResponse = { id: 1, name: 'Test User', email: 'test@example.test' };
  const project: Project = {
    id: 10,
    name: 'PMT',
    description: 'Gestion de projets',
    startDate: '2026-11-18',
    createdByUserId: user.id,
  };
  const projectService = {
    getProjects: vi.fn(),
    createProject: vi.fn(),
  };
  const authService = { currentUser: signal<AuthResponse | null>(user) };

  beforeEach(async () => {
    projectService.getProjects.mockReset().mockReturnValue(of([project]));
    projectService.createProject.mockReset().mockReturnValue(of(project));
    authService.currentUser.set(user);

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: ProjectService, useValue: projectService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should load the current user projects', () => {
    expect(component).toBeTruthy();
    expect(projectService.getProjects).toHaveBeenCalledWith(user.id);
    expect(component.projects()).toEqual([project]);
    expect(fixture.nativeElement.textContent).toContain('PMT');
  });

  it('should create a project and display it as an administrator', () => {
    const request = {
      name: 'Nouveau projet',
      description: 'Projet de test',
      startDate: '2026-11-20',
    };
    component.form = request;

    component.createProject();

    expect(projectService.createProject).toHaveBeenCalledWith(user.id, request);
    expect(component.projects()[0]).toEqual(project);
    expect(component.message()).toBe('Projet créé. Tu en es administrateur.');
    expect(component.saving()).toBe(false);
  });

  it('should not load projects without a logged-in user', async () => {
    projectService.getProjects.mockClear();
    authService.currentUser.set(null);
    const newFixture = TestBed.createComponent(Dashboard);
    newFixture.detectChanges();
    await newFixture.whenStable();

    expect(projectService.getProjects).not.toHaveBeenCalled();
    expect(newFixture.componentInstance.message()).toBe('Connecte-toi pour consulter tes projets.');
  });
});
