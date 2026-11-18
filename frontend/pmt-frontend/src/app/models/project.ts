export interface Project {
  id: number;
  name: string;
  description: string;
  startDate: string;
  createdByUserId: number;
}

export interface CreateProjectRequest {
  name: string;
  description: string;
  startDate: string;
}