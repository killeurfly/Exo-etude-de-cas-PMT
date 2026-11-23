import { DatePipe } from '@angular/common';
import { Component, computed, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Project } from '../../models/project';
import { InviteMemberRequest, ProjectMember, ProjectRole, ROLE_LABELS } from '../../models/project-member';
import {
  CreateTaskRequest,
  HISTORY_FIELD_LABELS,
  PRIORITY_LABELS,
  STATUS_LABELS,
  Task,
  TaskHistoryEntry,
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

/**
 * Page d'un projet (`/projects/:id`) : informations, tableau des tâches par statut, création,
 * modification et historique des tâches, membres et invitations.
 *
 * Ce qui est affiché dépend du rôle de l'utilisateur dans le projet (`currentRole`) : les formulaires
 * de tâches sont réservés aux rôles ADMIN et MEMBER, la gestion des membres aux ADMIN. Le backend
 * vérifie de toute façon ces droits.
 */
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
  readonly messageIsError = signal(false);
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
  readonly tasksByStatus = computed(() =>
    this.statuses.map((status) => ({
      status,
      tasks: this.tasks().filter((task) => task.status === status),
    })),
  );
  readonly taskMessage = signal('');
  readonly taskMessageIsError = signal(false);
  readonly creatingTask = signal(false);
  readonly canEditTasks = computed(() => this.currentRole() === 'ADMIN' || this.currentRole() === 'MEMBER');
  readonly assignableMembers = computed(() =>
    this.members().filter((member) => member.status === 'ACTIVE' && member.userId !== null),
  );
  taskForm: CreateTaskRequest = this.emptyTaskForm();
  readonly editingTaskId = signal<number | null>(null);
  readonly savingTask = signal(false);
  editForm: UpdateTaskRequest = { ...this.emptyTaskForm(), status: 'TODO' };
  readonly historyFieldLabels = HISTORY_FIELD_LABELS;
  readonly historyTaskId = signal<number | null>(null);
  readonly history = signal<TaskHistoryEntry[]>([]);
  readonly historyMessage = signal('');

  private projectId = 0;

  constructor(
    private readonly route: ActivatedRoute,
    readonly authService: AuthService,
    private readonly projectService: ProjectService,
    private readonly projectMemberService: ProjectMemberService,
    private readonly taskService: TaskService,
  ) {}

  /**
   * Lit l'identifiant du projet dans l'URL et charge en parallèle le projet, ses membres et ses tâches.
   */
  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    const user = this.authService.currentUser();
    if (user === null) {
      this.showMessage('Connecte-toi pour consulter ce projet.', true);
      return;
    }

    this.projectService.getProject(this.projectId, user.id).subscribe({
      next: (project) => this.project.set(project),
      error: (error: unknown) => {
        this.showMessage(getProjectErrorMessage(error, 'Impossible de charger le projet.'), true);
      },
    });
    this.loadMembers(user.id);
    this.taskService.getTasks(this.projectId, user.id).subscribe({
      next: (tasks) => this.tasks.set(tasks),
      error: (error: unknown) => {
        this.showTaskMessage(getProjectErrorMessage(error, 'Impossible de charger les tâches.'), true);
      },
    });
  }

  /** Crée la tâche saisie et l'ajoute en tête de liste. */
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
        this.showTaskMessage(`Tâche « ${task.name} » créée.`);
      },
      error: (error: unknown) => {
        this.creatingTask.set(false);
        this.showTaskMessage(getTaskErrorMessage(error), true);
      },
    });
  }

  /** Ouvre le formulaire de modification, prérempli avec la tâche. */
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

  /** Ferme le formulaire de modification sans enregistrer. */
  cancelEdit(): void {
    this.editingTaskId.set(null);
  }

  /**
   * Enregistre la tâche en cours de modification. En cas de refus, le formulaire reste ouvert
   * avec le message d'erreur.
   */
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
        this.showTaskMessage(`Tâche « ${updated.name} » modifiée.`);
      },
      error: (error: unknown) => {
        this.savingTask.set(false);
        this.showTaskMessage(getTaskErrorMessage(error, 'update'), true);
      },
    });
  }

  /** Affiche l'historique d'une tâche (chargé à la demande) ou le masque s'il est déjà ouvert. */
  toggleHistory(task: Task): void {
    const user = this.authService.currentUser();
    if (user === null) {
      return;
    }
    if (this.historyTaskId() === task.id) {
      this.historyTaskId.set(null);
      return;
    }

    this.historyTaskId.set(task.id);
    this.history.set([]);
    this.historyMessage.set('');
    this.taskService.getHistory(this.projectId, task.id, user.id).subscribe({
      next: (history) => this.history.set(history),
      error: (error: unknown) => {
        this.historyMessage.set(getProjectErrorMessage(error, 'Impossible de charger l’historique.'));
      },
    });
  }

  /** Traduit une valeur brute de l'historique (code, identifiant) en texte lisible. */
  historyValue(entry: TaskHistoryEntry, value: string | null): string {
    if (value === null || value === '') {
      return '—';
    }
    if (entry.fieldName === 'priority') {
      return this.priorityLabels[value as TaskPriority] ?? value;
    }
    if (entry.fieldName === 'status') {
      return this.statusLabels[value as TaskStatus] ?? value;
    }
    if (entry.fieldName === 'assignee') {
      const member = this.members().find((m) => m.userId === Number(value));
      return member?.name ?? member?.email ?? 'ancien membre';
    }
    return value;
  }

  /**
   * Envoie l'invitation saisie. Le message indique si la personne a rejoint le projet
   * ou si l'invitation attend son inscription.
   */
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
        this.showMessage(member.status === 'ACTIVE'
          ? `${member.email} a rejoint le projet.`
          : `Invitation enregistrée : ${member.email} rejoindra le projet à son inscription.`);
      },
      error: (error: unknown) => {
        this.inviting.set(false);
        this.showMessage(getInviteErrorMessage(error), true);
      },
    });
  }

  /**
   * Change le rôle d'un membre ; en cas de refus, la liste est rechargée pour réafficher le rôle réel.
   */
  changeRole(member: ProjectMember, role: ProjectRole): void {
    const user = this.authService.currentUser();
    if (user === null || role === member.role) {
      return;
    }

    this.message.set('');
    this.projectMemberService.updateRole(this.projectId, member.id, user.id, role).subscribe({
      next: (updated) => {
        this.members.update((members) => members.map((m) => (m.id === updated.id ? updated : m)));
        this.showMessage(`Rôle de ${updated.email} : ${this.roleLabels[updated.role]}.`);
      },
      error: (error: unknown) => {
        this.showMessage(getRoleErrorMessage(error), true);
        // Recharge la liste pour remettre le sélecteur sur le rôle réel.
        this.loadMembers(user.id);
      },
    });
  }

  /** Nom (ou e-mail) de la personne assignée à la tâche, `null` si personne. */
  assigneeName(task: Task): string | null {
    if (task.assigneeUserId === null) {
      return null;
    }
    const member = this.members().find((m) => m.userId === task.assigneeUserId);
    return member?.name ?? member?.email ?? null;
  }

  /** Affiche un message ; une erreur est mise en évidence en rouge. */
  private showMessage(text: string, isError = false): void {
    this.message.set(text);
    this.messageIsError.set(isError);
  }

  /** Affiche un message ; une erreur est mise en évidence en rouge. */
  private showTaskMessage(text: string, isError = false): void {
    this.taskMessage.set(text);
    this.taskMessageIsError.set(isError);
  }

  /** Valeurs initiales du formulaire de création de tâche. */
  private emptyTaskForm(): CreateTaskRequest {
    return { name: '', description: '', dueDate: null, priority: 'MEDIUM', assigneeUserId: null };
  }

  /** Charge la liste des membres du projet. */
  private loadMembers(userId: number): void {
    this.projectMemberService.getMembers(this.projectId, userId).subscribe({
      next: (members) => this.members.set(members),
      error: (error: unknown) => {
        this.showMessage(getProjectErrorMessage(error, 'Impossible de charger les membres.'), true);
      },
    });
  }
}
