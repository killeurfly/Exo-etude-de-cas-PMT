import { DatePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProjectMember } from '../../models/project-member';
import {
  HISTORY_FIELD_LABELS,
  PRIORITY_LABELS,
  STATUS_LABELS,
  Task,
  TaskHistoryEntry,
  TaskPriority,
  TaskStatus,
} from '../../models/task';
import { AuthService } from '../../services/auth.service';
import { ProjectMemberService } from '../../services/project-member.service';
import { getProjectErrorMessage } from '../../services/project.service';
import { TaskService } from '../../services/task.service';

@Component({
  selector: 'app-task',
  imports: [DatePipe, RouterLink],
  templateUrl: './task.html',
  styleUrl: './task.css',
})
export class TaskDetail implements OnInit {
  readonly priorityLabels = PRIORITY_LABELS;
  readonly statusLabels = STATUS_LABELS;
  readonly historyFieldLabels = HISTORY_FIELD_LABELS;

  readonly task = signal<Task | null>(null);
  readonly members = signal<ProjectMember[]>([]);
  readonly history = signal<TaskHistoryEntry[]>([]);
  readonly message = signal('');
  projectId = 0;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly authService: AuthService,
    private readonly taskService: TaskService,
    private readonly projectMemberService: ProjectMemberService,
  ) {}

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    const taskId = Number(this.route.snapshot.paramMap.get('taskId'));

    const user = this.authService.currentUser();
    if (user === null) {
      this.message.set('Connecte-toi pour consulter cette tâche.');
      return;
    }

    this.taskService.getTask(this.projectId, taskId, user.id).subscribe({
      next: (task) => this.task.set(task),
      error: (error: unknown) => {
        this.message.set(getProjectErrorMessage(error, 'Impossible de charger la tâche.'));
      },
    });
    this.projectMemberService.getMembers(this.projectId, user.id).subscribe({
      next: (members) => this.members.set(members),
    });
    this.taskService.getHistory(this.projectId, taskId, user.id).subscribe({
      next: (history) => this.history.set(history),
    });
  }

  // Les ids d'utilisateur sont traduits en noms grâce à la liste des membres.
  memberName(userId: number | null): string {
    if (userId === null) {
      return 'Personne';
    }
    const member = this.members().find((m) => m.userId === userId);
    return member?.name ?? member?.email ?? 'Ancien membre';
  }

  // Les valeurs de l'historique sont stockées brutes (codes, identifiants) : on les traduit pour l'affichage.
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
      return this.memberName(Number(value));
    }
    return value;
  }
}
