import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { InviteMemberRequest, ProjectMember, ProjectRole } from '../models/project-member';

@Injectable({
  providedIn: 'root',
})
export class ProjectMemberService {
  private readonly apiUrl = 'http://localhost:8080/api/projects';

  constructor(private readonly http: HttpClient) {}

  getMembers(projectId: number, userId: number): Observable<ProjectMember[]> {
    return this.http.get<ProjectMember[]>(this.membersUrl(projectId), {
      params: new HttpParams().set('userId', userId),
    });
  }

  inviteMember(projectId: number, userId: number, request: InviteMemberRequest): Observable<ProjectMember> {
    return this.http.post<ProjectMember>(this.membersUrl(projectId), request, {
      params: new HttpParams().set('userId', userId),
    });
  }

  updateRole(projectId: number, memberId: number, userId: number, role: ProjectRole): Observable<ProjectMember> {
    return this.http.put<ProjectMember>(`${this.membersUrl(projectId)}/${memberId}`, { role }, {
      params: new HttpParams().set('userId', userId),
    });
  }

  private membersUrl(projectId: number): string {
    return `${this.apiUrl}/${projectId}/members`;
  }
}

export function getInviteErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 400) {
      return 'Saisis une adresse e-mail valide et un rôle.';
    }
    if (error.status === 403) {
      return 'Seul un administrateur peut inviter des membres.';
    }
    if (error.status === 409) {
      return 'Cette personne fait déjà partie du projet.';
    }
  }

  return 'Impossible d’envoyer l’invitation.';
}

export function getRoleErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 403) {
      return 'Seul un administrateur peut modifier les rôles.';
    }
    if (error.status === 409) {
      return 'Le projet doit garder au moins un administrateur.';
    }
  }

  return 'Impossible de modifier le rôle.';
}
