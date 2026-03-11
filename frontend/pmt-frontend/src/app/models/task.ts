import { User } from './user';

export interface Task {
  id?: number;
  name: string;
  description: string;
  dueDate: string; // ISO string
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  assignee?: User;
  status?: 'TODO' | 'IN_PROGRESS' | 'DONE';
}