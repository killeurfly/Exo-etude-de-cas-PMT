import { Component, computed, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Project } from '../../models/project';
import { InviteMemberRequest, ProjectMember, ProjectRole, ROLE_LABELS } from '../../models/project-member';
import { AuthService } from '../../services/auth.service';
import {
  getInviteErrorMessage,
  getRoleErrorMessage,
  ProjectMemberService,
} from '../../services/project-member.service';
import { getProjectErrorMessage, ProjectService } from '../../services/project.service';

@Component({
  selector: 'app-project',
  imports: [FormsModule, RouterLink],
  templateUrl: './project.html',
  styleUrl: './project.css',
})
export class ProjectDetail implements OnInit {
  readonly roles: ProjectRole[] = ['ADMIN', 'MEMBER', 'OBSERVER'];
  readonly roleLabels = ROLE_LABELS;
  readonly project = signal<Project | null>(null);
  readonly members = signal<ProjectMember[]>([]);
  readonly message = signal('');
  readonly inviting = signal(false);
  readonly currentRole = computed(() => {
    const userId = this.authService.currentUser()?.id;
    return this.members().find((member) => member.userId === userId)?.role ?? null;
  });
  readonly isAdmin = computed(() => this.currentRole() === 'ADMIN');
  inviteForm: InviteMemberRequest = { email: '', role: 'MEMBER' };
  private projectId = 0;

  constructor(
    private readonly route: ActivatedRoute,
    readonly authService: AuthService,
    private readonly projectService: ProjectService,
    private readonly projectMemberService: ProjectMemberService,
  ) {}

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    const user = this.authService.currentUser();
    if (user === null) {
      this.message.set('Connecte-toi pour consulter ce projet.');
      return;
    }

    this.projectService.getProject(this.projectId, user.id).subscribe({
      next: (project) => this.project.set(project),
      error: (error: unknown) => {
        this.message.set(getProjectErrorMessage(error, 'Impossible de charger le projet.'));
      },
    });
    this.loadMembers(user.id);
  }

  invite(): void {
    const user = this.authService.currentUser();
    if (user === null) {
      return;
    }

    this.message.set('');
    this.inviting.set(true);
    this.projectMemberService.inviteMember(this.projectId, user.id, this.inviteForm).subscribe({
      next: (member) => {
        this.members.update((members) => [...members, member]);
        this.inviteForm = { email: '', role: 'MEMBER' };
        this.inviting.set(false);
        this.message.set(member.status === 'ACTIVE'
          ? `${member.email} a rejoint le projet.`
          : `Invitation enregistrée : ${member.email} rejoindra le projet à son inscription.`);
      },
      error: (error: unknown) => {
        this.inviting.set(false);
        this.message.set(getInviteErrorMessage(error));
      },
    });
  }

  changeRole(member: ProjectMember, role: ProjectRole): void {
    const user = this.authService.currentUser();
    if (user === null || role === member.role) {
      return;
    }

    this.message.set('');
    this.projectMemberService.updateRole(this.projectId, member.id, user.id, role).subscribe({
      next: (updated) => {
        this.members.update((members) => members.map((m) => (m.id === updated.id ? updated : m)));
        this.message.set(`Rôle de ${updated.email} : ${this.roleLabels[updated.role]}.`);
      },
      error: (error: unknown) => {
        this.message.set(getRoleErrorMessage(error));
        // Recharge la liste pour remettre le sélecteur sur le rôle réel.
        this.loadMembers(user.id);
      },
    });
  }

  private loadMembers(userId: number): void {
    this.projectMemberService.getMembers(this.projectId, userId).subscribe({
      next: (members) => this.members.set(members),
      error: (error: unknown) => {
        this.message.set(getProjectErrorMessage(error, 'Impossible de charger les membres.'));
      },
    });
  }
}
