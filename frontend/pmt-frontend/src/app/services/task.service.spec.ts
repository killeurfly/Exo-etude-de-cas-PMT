import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { getTaskErrorMessage, TaskService } from './task.service';

describe('TaskService', () => {
  const tasksUrl = 'http://localhost:8080/api/projects/10/tasks';
  let service: TaskService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TaskService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should call the task endpoints with the user id', () => {
    const request = {
      name: 'Tâche', description: 'Test', dueDate: null, priority: 'LOW' as const, assigneeUserId: null,
    };

    service.getTasks(10, 1).subscribe();
    service.getTask(10, 50, 1).subscribe();
    service.getHistory(10, 50, 1).subscribe();
    service.createTask(10, 1, request).subscribe();
    service.updateTask(10, 50, 1, { ...request, status: 'DONE' }).subscribe();

    httpTestingController.expectOne({ method: 'GET', url: `${tasksUrl}?userId=1` });
    httpTestingController.expectOne({ method: 'GET', url: `${tasksUrl}/50?userId=1` });
    httpTestingController.expectOne({ method: 'GET', url: `${tasksUrl}/50/history?userId=1` });
    const create = httpTestingController.expectOne({ method: 'POST', url: `${tasksUrl}?userId=1` });
    expect(create.request.body).toEqual(request);
    const update = httpTestingController.expectOne({ method: 'PUT', url: `${tasksUrl}/50?userId=1` });
    expect(update.request.body.status).toBe('DONE');
  });

  it('should translate task errors for creation and update', () => {
    const error = (status: number) => new HttpErrorResponse({ status });

    expect(getTaskErrorMessage(error(400))).toContain('obligatoires');
    expect(getTaskErrorMessage(error(403))).toBe('Ton rôle ne permet pas de créer une tâche dans ce projet.');
    expect(getTaskErrorMessage(error(403), 'update')).toBe('Ton rôle ne permet pas de modifier une tâche dans ce projet.');
    expect(getTaskErrorMessage(error(404), 'update')).toBe('Cette tâche n’existe plus dans ce projet.');
    expect(getTaskErrorMessage(error(500))).toBe('Impossible de créer la tâche.');
    expect(getTaskErrorMessage(new Error('réseau'), 'update')).toBe('Impossible de modifier la tâche.');
  });
});
