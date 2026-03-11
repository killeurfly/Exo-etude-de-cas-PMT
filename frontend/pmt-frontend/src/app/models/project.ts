import { User } from './user';

export interface Project {
  id?: number;
  name: string;
  description: string;
  startDate: string; // ISO string
  members?: User[];
}