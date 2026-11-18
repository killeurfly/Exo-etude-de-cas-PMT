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
  createdByUserId: number | null;
}

export interface CreateTaskRequest {
  name: string;
  description: string;
  dueDate: string | null;
  priority: TaskPriority;
}

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
