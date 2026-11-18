import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthResponse } from '../../models/auth';
import { Project } from '../../models/project';
import { ProjectMember } from '../../models/project-member';
import { AuthService } from '../../services/auth.service';
import { ProjectMemberService } from '../../services/project-member.service';
import { ProjectService } from '../../services/project.service';
import { ProjectDetail } from './project';

describe('ProjectDetail', () => {
  let component: ProjectDetail;
  let fixture: ComponentFixture<ProjectDetail>;
  const admin: AuthResponse = { id: 1, name: 'Admin', email: 'admin@example.test' };
  const project: Project = {
    id: 10,
    name: 'PMT',
    description: 'Gestion de projets',
    startDate: '2026-11-18',
    createdByUserId: admin.id,
  };
  const adminMember: ProjectMember = {
    id: 100, userId: admin.id, name: 'Admin', email: admin.email, role: 'ADMIN', status: 'ACTIVE',
  };
  const bobMember: ProjectMember = {
    id: 101, userId: 2, name: 'Bob', email: 'bob@example.test', role: 'MEMBER', status: 'ACTIVE',
  };
  const authService = { currentUser: signal<AuthResponse | null>(admin) };
  const projectService = { getProject: vi.fn() };
  const memberService = { getMembers: vi.fn(), inviteMember: vi.fn(), updateRole: vi.fn() };

  async function createComponent(members: ProjectMember[]): Promise<void> {
    memberService.getMembers.mockReturnValue(of(members));
    fixture = TestBed.createComponent(ProjectDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  }

  beforeEach(async () => {
    authService.currentUser.set(admin);
    projectService.getProject.mockReset().mockReturnValue(of(project));
    memberService.getMembers.mockReset();
    memberService.inviteMember.mockReset();
    memberService.updateRole.mockReset();

    await TestBed.configureTestingModule({
      imports: [ProjectDetail],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '10' }) } } },
        { provide: AuthService, useValue: authService },
        { provide: ProjectService, useValue: projectService },
        { provide: ProjectMemberService, useValue: memberService },
      ],
    }).compileComponents();
  });

  it('should load the project and its members', async () => {
    await createComponent([adminMember, bobMember]);

    expect(projectService.getProject).toHaveBeenCalledWith(10, admin.id);
    expect(memberService.getMembers).toHaveBeenCalledWith(10, admin.id);
    expect(component.currentRole()).toBe('ADMIN');
    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('PMT');
    expect(text).toContain('Bob');
  });

  it('should show the invitation form only to administrators', async () => {
    await createComponent([{ ...adminMember, role: 'OBSERVER' }]);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(component.isAdmin()).toBe(false);
    expect(compiled.querySelector('#invite-email')).toBeNull();
    expect(compiled.querySelector('select')).toBeNull();
  });

  it('should add a pending invitation to the members list', async () => {
    await createComponent([adminMember]);
    const invited: ProjectMember = {
      id: 102, userId: null, name: null, email: 'carol@example.test', role: 'OBSERVER', status: 'INVITED',
    };
    memberService.inviteMember.mockReturnValue(of(invited));
    component.inviteForm = { email: 'carol@example.test', role: 'OBSERVER' };

    component.invite();

    expect(memberService.inviteMember).toHaveBeenCalledWith(10, admin.id, {
      email: 'carol@example.test',
      role: 'OBSERVER',
    });
    expect(component.members()).toEqual([adminMember, invited]);
    expect(component.message()).toContain('rejoindra le projet à son inscription');
  });

  it('should explain why an invitation is refused', async () => {
    await createComponent([adminMember]);
    memberService.inviteMember.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    component.inviteForm = { email: 'bob@example.test', role: 'MEMBER' };

    component.invite();

    expect(component.message()).toBe('Cette personne fait déjà partie du projet.');
    expect(component.inviting()).toBe(false);
  });

  it('should update a member role', async () => {
    await createComponent([adminMember, bobMember]);
    memberService.updateRole.mockReturnValue(of({ ...bobMember, role: 'OBSERVER' }));

    component.changeRole(bobMember, 'OBSERVER');

    expect(memberService.updateRole).toHaveBeenCalledWith(10, bobMember.id, admin.id, 'OBSERVER');
    expect(component.members()[1].role).toBe('OBSERVER');
  });

  it('should keep the last administrator', async () => {
    await createComponent([adminMember]);
    memberService.updateRole.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.changeRole(adminMember, 'MEMBER');

    expect(component.message()).toBe('Le projet doit garder au moins un administrateur.');
    expect(memberService.getMembers).toHaveBeenCalledTimes(2);
  });
});
