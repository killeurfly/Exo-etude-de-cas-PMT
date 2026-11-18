export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export interface Task {
  id: number;
  projectId: number;
  name: string;
  description: string;
  dueDate: string | null;
  priority: TaskPriority;
  status: TaskStatus;
  completedAt: string | null;
  createdByUserId: number | null;
  assigneeUserId: number | null;
}

export interface CreateTaskRequest {
  name: string;
  description: string;
  dueDate: string | null;
  priority: TaskPriority;
  assigneeUserId: number | null;
}

export interface UpdateTaskRequest extends CreateTaskRequest {
  status: TaskStatus;
}

export interface TaskHistoryEntry {
  id: number;
  fieldName: string;
  oldValue: string | null;
  newValue: string | null;
  changedByUserId: number | null;
  changedByName: string | null;
  changedAt: string;
}

export const HISTORY_FIELD_LABELS: Partial<Record<string, string>> = {
  name: 'Nom',
  description: 'Description',
  dueDate: 'Échéance',
  priority: 'Priorité',
  status: 'Statut',
  assignee: 'Assignée à',
};

export const PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: 'Basse',
  MEDIUM: 'Moyenne',
  HIGH: 'Haute',
};

export const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'À faire',
  IN_PROGRESS: 'En cours',
  DONE: 'Terminée',
};
