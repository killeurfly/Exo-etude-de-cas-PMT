export type ProjectRole = 'ADMIN' | 'MEMBER' | 'OBSERVER';

export type ProjectMemberStatus = 'INVITED' | 'ACTIVE';

export interface ProjectMember {
  id: number;
  userId: number | null;
  name: string | null;
  email: string;
  role: ProjectRole;
  status: ProjectMemberStatus;
}

export interface InviteMemberRequest {
  email: string;
  role: ProjectRole;
}

export const ROLE_LABELS: Record<ProjectRole, string> = {
  ADMIN: 'Administrateur',
  MEMBER: 'Membre',
  OBSERVER: 'Observateur',
};
