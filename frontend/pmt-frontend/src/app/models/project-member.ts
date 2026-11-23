/** Rôle dans un projet : ADMIN (tout), MEMBER (gérer les tâches), OBSERVER (lecture seule). */
export type ProjectRole = 'ADMIN' | 'MEMBER' | 'OBSERVER';

/** INVITED : invitation envoyée à un e-mail sans compte ; ACTIVE : membre inscrit. */
export type ProjectMemberStatus = 'INVITED' | 'ACTIVE';

/** Membre d'un projet ou invitation en attente (`userId` et `name` sont alors `null`). */
export interface ProjectMember {
  id: number;
  userId: number | null;
  name: string | null;
  email: string;
  role: ProjectRole;
  status: ProjectMemberStatus;
}

/** Données d'une invitation. */
export interface InviteMemberRequest {
  email: string;
  role: ProjectRole;
}

/** Libellés des rôles affichés à l'écran. */
export const ROLE_LABELS: Record<ProjectRole, string> = {
  ADMIN: 'Administrateur',
  MEMBER: 'Membre',
  OBSERVER: 'Observateur',
};
