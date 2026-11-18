import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthResponse } from '../../models/auth';
import { Project } from '../../models/project';
import { ProjectMember } from '../../models/project-member';
import { Task } from '../../models/task';
import { AuthService } from '../../services/auth.service';
import { ProjectMemberService } from '../../services/project-member.service';
import { ProjectService } from '../../services/project.service';
import { TaskService } from '../../services/task.service';
import { ProjectDetail } from './project';

describe('ProjectDetail', () => {
  let component: ProjectDetail;
  let fixture: ComponentFixture<ProjectDetail>;
  const admin: AuthResponse = { id: 1, name: 'Admin', email: 'admin@example.test' };
  const project: Project = {
    id: 10,
    name: 'PMT',
    description: 'Gestion de projets',
    startDate: '2026-11-18',
    createdByUserId: admin.id,
  };
  const adminMember: ProjectMember = {
    id: 100, userId: admin.id, name: 'Admin', email: admin.email, role: 'ADMIN', status: 'ACTIVE',
  };
  const bobMember: ProjectMember = {
    id: 101, userId: 2, name: 'Bob', email: 'bob@example.test', role: 'MEMBER', status: 'ACTIVE',
  };
  const authService = { currentUser: signal<AuthResponse | null>(admin) };
  const projectService = { getProject: vi.fn() };
  const memberService = { getMembers: vi.fn(), inviteMember: vi.fn(), updateRole: vi.fn() };
  const existingTask: Task = {
    id: 50,
    projectId: project.id,
    name: 'Concevoir le modèle',
    description: 'Définir les entités',
    dueDate: '2026-11-25',
    priority: 'HIGH',
    status: 'IN_PROGRESS',
    completedAt: null,
    createdByUserId: admin.id,
    assigneeUserId: null,
  };
  const taskService = { getTasks: vi.fn(), createTask: vi.fn(), updateTask: vi.fn() };

  async function createComponent(members: ProjectMember[]): Promise<void> {
    memberService.getMembers.mockReturnValue(of(members));
    fixture = TestBed.createComponent(ProjectDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  }

  beforeEach(async () => {
    authService.currentUser.set(admin);
    projectService.getProject.mockReset().mockReturnValue(of(project));
    memberService.getMembers.mockReset();
    memberService.inviteMember.mockReset();
    memberService.updateRole.mockReset();
    taskService.getTasks.mockReset().mockReturnValue(of([existingTask]));
    taskService.createTask.mockReset();
    taskService.updateTask.mockReset();

    await TestBed.configureTestingModule({
      imports: [ProjectDetail],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '10' }) } } },
        { provide: AuthService, useValue: authService },
        { provide: ProjectService, useValue: projectService },
        { provide: ProjectMemberService, useValue: memberService },
        { provide: TaskService, useValue: taskService },
      ],
    }).compileComponents();
  });

  it('should load the project and its members', async () => {
    await createComponent([adminMember, bobMember]);

    expect(projectService.getProject).toHaveBeenCalledWith(10, admin.id);
    expect(memberService.getMembers).toHaveBeenCalledWith(10, admin.id);
    expect(component.currentRole()).toBe('ADMIN');
    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('PMT');
    expect(text).toContain('Bob');
  });

  it('should show the invitation form only to administrators', async () => {
    await createComponent([{ ...adminMember, role: 'OBSERVER' }]);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(component.isAdmin()).toBe(false);
    expect(compiled.querySelector('#invite-email')).toBeNull();
    expect(compiled.querySelector('select')).toBeNull();
  });

  it('should add a pending invitation to the members list', async () => {
    await createComponent([adminMember]);
    const invited: ProjectMember = {
      id: 102, userId: null, name: null, email: 'carol@example.test', role: 'OBSERVER', status: 'INVITED',
    };
    memberService.inviteMember.mockReturnValue(of(invited));
    component.inviteForm = { email: 'carol@example.test', role: 'OBSERVER' };

    component.invite();

    expect(memberService.inviteMember).toHaveBeenCalledWith(10, admin.id, {
      email: 'carol@example.test',
      role: 'OBSERVER',
    });
    expect(component.members()).toEqual([adminMember, invited]);
    expect(component.message()).toContain('rejoindra le projet à son inscription');
  });

  it('should explain why an invitation is refused', async () => {
    await createComponent([adminMember]);
    memberService.inviteMember.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    component.inviteForm = { email: 'bob@example.test', role: 'MEMBER' };

    component.invite();

    expect(component.message()).toBe('Cette personne fait déjà partie du projet.');
    expect(component.inviting()).toBe(false);
  });

  it('should update a member role', async () => {
    await createComponent([adminMember, bobMember]);
    memberService.updateRole.mockReturnValue(of({ ...bobMember, role: 'OBSERVER' }));

    component.changeRole(bobMember, 'OBSERVER');

    expect(memberService.updateRole).toHaveBeenCalledWith(10, bobMember.id, admin.id, 'OBSERVER');
    expect(component.members()[1].role).toBe('OBSERVER');
  });

  it('should load and display the project tasks', async () => {
    await createComponent([adminMember]);

    expect(taskService.getTasks).toHaveBeenCalledWith(10, admin.id);
    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('Concevoir le modèle');
    expect(text).toContain('En cours');
    expect(text).toContain('Haute');
  });

  it('should group tasks by status with a count per column', async () => {
    const todo: Task = { ...existingTask, id: 51, name: 'À démarrer', status: 'TODO' };
    const done: Task = { ...existingTask, id: 52, name: 'Livrée', status: 'DONE' };
    taskService.getTasks.mockReturnValue(of([todo, existingTask, done]));
    await createComponent([adminMember]);

    expect(component.tasksByStatus().map((column) => column.tasks.map((task) => task.id))).toEqual([
      [51], [50], [52],
    ]);
    const columns = (fixture.nativeElement as HTMLElement).querySelectorAll('.task-column');
    expect(columns.length).toBe(3);
    expect(columns[0].textContent).toContain('À faire (1)');
    expect(columns[1].textContent).toContain('Concevoir le modèle');
    expect(columns[2].textContent).toContain('Livrée');
  });

  it('should let members create tasks but hide the form from observers', async () => {
    await createComponent([{ ...adminMember, role: 'MEMBER' }]);
    expect(component.canEditTasks()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).querySelector('#task-name')).not.toBeNull();

    await createComponent([{ ...adminMember, role: 'OBSERVER' }]);
    expect(component.canEditTasks()).toBe(false);
    expect((fixture.nativeElement as HTMLElement).querySelector('#task-name')).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Concevoir le modèle');
  });

  it('should create a task without due date and show it first', async () => {
    await createComponent([adminMember]);
    const created: Task = {
      ...existingTask, id: 51, name: 'Nouvelle tâche', dueDate: null, priority: 'LOW', status: 'TODO',
    };
    taskService.createTask.mockReturnValue(of(created));
    // Un champ date vidé dans le navigateur vaut ''.
    component.taskForm = {
      name: 'Nouvelle tâche', description: 'Test', dueDate: '', priority: 'LOW', assigneeUserId: null,
    };

    component.createTask();

    expect(taskService.createTask).toHaveBeenCalledWith(10, admin.id, {
      name: 'Nouvelle tâche',
      description: 'Test',
      dueDate: null,
      priority: 'LOW',
      assigneeUserId: null,
    });
    expect(component.tasks()).toEqual([created, existingTask]);
    expect(component.taskForm.name).toBe('');
    expect(component.taskMessage()).toBe('Tâche « Nouvelle tâche » créée.');
  });

  it('should explain a refused task creation', async () => {
    await createComponent([adminMember]);
    taskService.createTask.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));

    component.createTask();

    expect(component.taskMessage()).toBe('Ton rôle ne permet pas de créer une tâche dans ce projet.');
    expect(component.creatingTask()).toBe(false);
    expect(component.tasks()).toEqual([existingTask]);
  });

  it('should prefill the edit form with the selected task', async () => {
    await createComponent([adminMember]);

    component.startEdit(existingTask);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.editingTaskId()).toBe(existingTask.id);
    expect(component.editForm).toEqual({
      name: 'Concevoir le modèle',
      description: 'Définir les entités',
      dueDate: '2026-11-25',
      priority: 'HIGH',
      status: 'IN_PROGRESS',
      assigneeUserId: null,
    });
    expect((fixture.nativeElement as HTMLElement).querySelector('#edit-status')).not.toBeNull();
  });

  it('should save a finished task and show its completion date', async () => {
    await createComponent([adminMember]);
    const finished: Task = { ...existingTask, status: 'DONE', completedAt: '2026-11-18T12:45:00' };
    taskService.updateTask.mockReturnValue(of(finished));
    component.startEdit(existingTask);
    component.editForm.status = 'DONE';
    component.editForm.dueDate = '';

    component.saveTask();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(taskService.updateTask).toHaveBeenCalledWith(10, existingTask.id, admin.id, {
      name: 'Concevoir le modèle',
      description: 'Définir les entités',
      dueDate: null,
      priority: 'HIGH',
      status: 'DONE',
      assigneeUserId: null,
    });
    expect(component.tasks()).toEqual([finished]);
    expect(component.editingTaskId()).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('terminée le 18/11/2026 à 12:45');
  });

  it('should keep the form open when the update is refused', async () => {
    await createComponent([adminMember]);
    taskService.updateTask.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    component.startEdit(existingTask);

    component.saveTask();

    expect(component.taskMessage()).toBe('Ton rôle ne permet pas de modifier une tâche dans ce projet.');
    expect(component.editingTaskId()).toBe(existingTask.id);
    expect(component.tasks()).toEqual([existingTask]);
  });

  it('should assign a task to an active member and show their name', async () => {
    const invited: ProjectMember = {
      id: 102, userId: null, name: null, email: 'carol@example.test', role: 'MEMBER', status: 'INVITED',
    };
    await createComponent([adminMember, bobMember, invited]);
    expect(component.assignableMembers()).toEqual([adminMember, bobMember]);

    const assigned: Task = { ...existingTask, assigneeUserId: bobMember.userId };
    taskService.updateTask.mockReturnValue(of(assigned));
    component.startEdit(existingTask);
    component.editForm.assigneeUserId = bobMember.userId;

    component.saveTask();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(taskService.updateTask.mock.calls[0][3].assigneeUserId).toBe(bobMember.userId);
    expect(component.assigneeName(assigned)).toBe('Bob');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('assignée à Bob');
  });

  it('should not offer task editing to observers', async () => {
    await createComponent([{ ...adminMember, role: 'OBSERVER' }]);

    const buttons = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'));
    expect(buttons.some((button) => button.textContent?.includes('Modifier'))).toBe(false);
  });

  it('should keep the last administrator', async () => {
    await createComponent([adminMember]);
    memberService.updateRole.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.changeRole(adminMember, 'MEMBER');

    expect(component.message()).toBe('Le projet doit garder au moins un administrateur.');
    expect(memberService.getMembers).toHaveBeenCalledTimes(2);
  });
});
