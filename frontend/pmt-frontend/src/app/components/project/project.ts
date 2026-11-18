import { DatePipe } from '@angular/common';
import { Component, computed, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Project } from '../../models/project';
import { InviteMemberRequest, ProjectMember, ProjectRole, ROLE_LABELS } from '../../models/project-member';
import {
  CreateTaskRequest,
  PRIORITY_LABELS,
  STATUS_LABELS,
  Task,
  TaskPriority,
  TaskStatus,
  UpdateTaskRequest,
} from '../../models/task';
import { AuthService } from '../../services/auth.service';
import {
  getInviteErrorMessage,
  getRoleErrorMessage,
  ProjectMemberService,
} from '../../services/project-member.service';
import { getProjectErrorMessage, ProjectService } from '../../services/project.service';
import { getTaskErrorMessage, TaskService } from '../../services/task.service';

@Component({
  selector: 'app-project',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './project.html',
  styleUrl: './project.css',
})
export class ProjectDetail implements OnInit {
  readonly roles: ProjectRole[] = ['ADMIN', 'MEMBER', 'OBSERVER'];
  readonly roleLabels = ROLE_LABELS;
  readonly project = signal<Project | null>(null);
  readonly members = signal<ProjectMember[]>([]);
  readonly message = signal('');
  readonly inviting = signal(false);
  readonly currentRole = computed(() => {
    const userId = this.authService.currentUser()?.id;
    return this.members().find((member) => member.userId === userId)?.role ?? null;
  });
  readonly isAdmin = computed(() => this.currentRole() === 'ADMIN');
  inviteForm: InviteMemberRequest = { email: '', role: 'MEMBER' };

  readonly priorities: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH'];
  readonly statuses: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];
  readonly priorityLabels = PRIORITY_LABELS;
  readonly statusLabels = STATUS_LABELS;
  readonly tasks = signal<Task[]>([]);
  readonly taskMessage = signal('');
  readonly creatingTask = signal(false);
  readonly canEditTasks = computed(() => this.currentRole() === 'ADMIN' || this.currentRole() === 'MEMBER');
  readonly assignableMembers = computed(() =>
    this.members().filter((member) => member.status === 'ACTIVE' && member.userId !== null),
  );
  taskForm: CreateTaskRequest = this.emptyTaskForm();
  readonly editingTaskId = signal<number | null>(null);
  readonly savingTask = signal(false);
  editForm: UpdateTaskRequest = { ...this.emptyTaskForm(), status: 'TODO' };

  private projectId = 0;

  constructor(
    private readonly route: ActivatedRoute,
    readonly authService: AuthService,
    private readonly projectService: ProjectService,
    private readonly projectMemberService: ProjectMemberService,
    private readonly taskService: TaskService,
  ) {}

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    const user = this.authService.currentUser();
    if (user === null) {
      this.message.set('Connecte-toi pour consulter ce projet.');
      return;
    }

    this.projectService.getProject(this.projectId, user.id).subscribe({
      next: (project) => this.project.set(project),
      error: (error: unknown) => {
        this.message.set(getProjectErrorMessage(error, 'Impossible de charger le projet.'));
      },
    });
    this.loadMembers(user.id);
    this.taskService.getTasks(this.projectId, user.id).subscribe({
      next: (tasks) => this.tasks.set(tasks),
      error: (error: unknown) => {
        this.taskMessage.set(getProjectErrorMessage(error, 'Impossible de charger les tâches.'));
      },
    });
  }

  createTask(): void {
    const user = this.authService.currentUser();
    if (user === null) {
      return;
    }

    this.taskMessage.set('');
    this.creatingTask.set(true);
    // Un champ date vidé renvoie '' : on envoie null pour « pas d'échéance ».
    const request = { ...this.taskForm, dueDate: this.taskForm.dueDate || null };
    this.taskService.createTask(this.projectId, user.id, request).subscribe({
      next: (task) => {
        this.tasks.update((tasks) => [task, ...tasks]);
        this.taskForm = this.emptyTaskForm();
        this.creatingTask.set(false);
        this.taskMessage.set(`Tâche « ${task.name} » créée.`);
      },
      error: (error: unknown) => {
        this.creatingTask.set(false);
        this.taskMessage.set(getTaskErrorMessage(error));
      },
    });
  }

  startEdit(task: Task): void {
    this.taskMessage.set('');
    this.editForm = {
      name: task.name,
      description: task.description,
      dueDate: task.dueDate,
      priority: task.priority,
      status: task.status,
      assigneeUserId: task.assigneeUserId,
    };
    this.editingTaskId.set(task.id);
  }

  cancelEdit(): void {
    this.editingTaskId.set(null);
  }

  saveTask(): void {
    const user = this.authService.currentUser();
    const taskId = this.editingTaskId();
    if (user === null || taskId === null) {
      return;
    }

    this.taskMessage.set('');
    this.savingTask.set(true);
    const request = { ...this.editForm, dueDate: this.editForm.dueDate || null };
    this.taskService.updateTask(this.projectId, taskId, user.id, request).subscribe({
      next: (updated) => {
        this.tasks.update((tasks) => tasks.map((task) => (task.id === updated.id ? updated : task)));
        this.editingTaskId.set(null);
        this.savingTask.set(false);
        this.taskMessage.set(`Tâche « ${updated.name} » modifiée.`);
      },
      error: (error: unknown) => {
        this.savingTask.set(false);
        this.taskMessage.set(getTaskErrorMessage(error, 'update'));
      },
    });
  }

  invite(): void {
    const user = this.authService.currentUser();
    if (user === null) {
      return;
    }

    this.message.set('');
    this.inviting.set(true);
    this.projectMemberService.inviteMember(this.projectId, user.id, this.inviteForm).subscribe({
      next: (member) => {
        this.members.update((members) => [...members, member]);
        this.inviteForm = { email: '', role: 'MEMBER' };
        this.inviting.set(false);
        this.message.set(member.status === 'ACTIVE'
          ? `${member.email} a rejoint le projet.`
          : `Invitation enregistrée : ${member.email} rejoindra le projet à son inscription.`);
      },
      error: (error: unknown) => {
        this.inviting.set(false);
        this.message.set(getInviteErrorMessage(error));
      },
    });
  }

  changeRole(member: ProjectMember, role: ProjectRole): void {
    const user = this.authService.currentUser();
    if (user === null || role === member.role) {
      return;
    }

    this.message.set('');
    this.projectMemberService.updateRole(this.projectId, member.id, user.id, role).subscribe({
      next: (updated) => {
        this.members.update((members) => members.map((m) => (m.id === updated.id ? updated : m)));
        this.message.set(`Rôle de ${updated.email} : ${this.roleLabels[updated.role]}.`);
      },
      error: (error: unknown) => {
        this.message.set(getRoleErrorMessage(error));
        // Recharge la liste pour remettre le sélecteur sur le rôle réel.
        this.loadMembers(user.id);
      },
    });
  }

  assigneeName(task: Task): string | null {
    if (task.assigneeUserId === null) {
      return null;
    }
    const member = this.members().find((m) => m.userId === task.assigneeUserId);
    return member?.name ?? member?.email ?? null;
  }

  private emptyTaskForm(): CreateTaskRequest {
    return { name: '', description: '', dueDate: null, priority: 'MEDIUM', assigneeUserId: null };
  }

  private loadMembers(userId: number): void {
    this.projectMemberService.getMembers(this.projectId, userId).subscribe({
      next: (members) => this.members.set(members),
      error: (error: unknown) => {
        this.message.set(getProjectErrorMessage(error, 'Impossible de charger les membres.'));
      },
    });
  }
}
