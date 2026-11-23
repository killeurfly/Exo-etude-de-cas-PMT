import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ProjectMember } from '../../models/project-member';
import { Task, TaskHistoryEntry } from '../../models/task';
import { AuthService } from '../../services/auth.service';
import { ProjectMemberService } from '../../services/project-member.service';
import { TaskService } from '../../services/task.service';
import { TaskDetail } from './task';

describe('TaskDetail', () => {
  let fixture: ComponentFixture<TaskDetail>;
  const task: Task = {
    id: 50,
    projectId: 10,
    name: 'Concevoir le modèle',
    description: 'Définir les entités',
    dueDate: '2026-11-25',
    priority: 'HIGH',
    status: 'DONE',
    completedAt: '2026-11-20T09:30:00',
    createdByUserId: 1,
    assigneeUserId: 2,
  };
  const members: ProjectMember[] = [
    { id: 100, userId: 1, name: 'Admin', email: 'admin@example.test', role: 'ADMIN', status: 'ACTIVE' },
    { id: 101, userId: 2, name: 'Bob', email: 'bob@example.test', role: 'OBSERVER', status: 'ACTIVE' },
  ];
  const history: TaskHistoryEntry[] = [
    {
      id: 1, fieldName: 'status', oldValue: 'IN_PROGRESS', newValue: 'DONE',
      changedByUserId: 1, changedByName: 'Admin', changedAt: '2026-11-20T09:30:00',
    },
  ];
  const taskService = { getTask: vi.fn(), getHistory: vi.fn() };
  const memberService = { getMembers: vi.fn() };

  beforeEach(async () => {
    taskService.getTask.mockReset().mockReturnValue(of(task));
    taskService.getHistory.mockReset().mockReturnValue(of(history));
    memberService.getMembers.mockReset().mockReturnValue(of(members));

    await TestBed.configureTestingModule({
      imports: [TaskDetail],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '10', taskId: '50' }) } },
        },
        {
          provide: AuthService,
          useValue: { currentUser: signal({ id: 1, name: 'Admin', email: 'admin@example.test' }) },
        },
        { provide: TaskService, useValue: taskService },
        { provide: ProjectMemberService, useValue: memberService },
      ],
    }).compileComponents();
  });

  async function render(): Promise<HTMLElement> {
    fixture = TestBed.createComponent(TaskDetail);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('should load and display every task detail', async () => {
    const page = await render();

    expect(taskService.getTask).toHaveBeenCalledWith(10, 50, 1);
    expect(taskService.getHistory).toHaveBeenCalledWith(10, 50, 1);
    const text = page.textContent;
    expect(text).toContain('Concevoir le modèle');
    expect(text).toContain('Définir les entités');
    expect(text).toContain('Terminée');
    expect(text).toContain('Haute');
    expect(text).toContain('25/11/2026');
    expect(text).toContain('Bob');
    expect(text).toContain('Admin');
    expect(text).toContain('20/11/2026 à 09:30');
    expect(text).toContain('En cours → Terminée');
  });

  it('should explain why the task cannot be displayed', async () => {
    taskService.getTask.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));

    const page = await render();

    expect(page.textContent).toContain('Tu ne fais pas partie de ce projet.');
    expect(page.querySelector('h2')).toBeNull();
  });
});
