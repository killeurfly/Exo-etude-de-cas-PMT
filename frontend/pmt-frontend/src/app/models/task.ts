export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

/**
 * Tâche telle que renvoyée par l'API. `completedAt` est renseignée par le backend au passage à DONE ;
 * `assigneeUserId` vaut `null` si personne n'est assigné.
 */
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

/**
 * Données de création d'une tâche ; `dueDate` et `assigneeUserId` sont facultatifs (`null`).
 */
export interface CreateTaskRequest {
  name: string;
  description: string;
  dueDate: string | null;
  priority: TaskPriority;
  assigneeUserId: number | null;
}

/** Nouvelles valeurs d'une tâche : tous les champs sont remplacés, statut compris. */
export interface UpdateTaskRequest extends CreateTaskRequest {
  status: TaskStatus;
}

/**
 * Modification d'un champ d'une tâche. Les valeurs sont brutes (code, date ISO ou identifiant
 * de l'utilisateur pour `assignee`) et doivent être traduites pour l'affichage.
 */
export interface TaskHistoryEntry {
  id: number;
  fieldName: string;
  oldValue: string | null;
  newValue: string | null;
  changedByUserId: number | null;
  changedByName: string | null;
  changedAt: string;
}

/** Libellés des champs de l'historique affichés à l'écran. */
export const HISTORY_FIELD_LABELS: Partial<Record<string, string>> = {
  name: 'Nom',
  description: 'Description',
  dueDate: 'Échéance',
  priority: 'Priorité',
  status: 'Statut',
  assignee: 'Assignée à',
};

/** Libellés des priorités affichés à l'écran. */
export const PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: 'Basse',
  MEDIUM: 'Moyenne',
  HIGH: 'Haute',
};

/** Libellés des statuts, utilisés aussi comme titres des colonnes du tableau de bord. */
export const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'À faire',
  IN_PROGRESS: 'En cours',
  DONE: 'Terminée',
};
