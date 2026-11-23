import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { getInviteErrorMessage, getRoleErrorMessage, ProjectMemberService } from './project-member.service';

describe('ProjectMemberService', () => {
  const membersUrl = 'http://localhost:8080/api/projects/10/members';
  let service: ProjectMemberService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProjectMemberService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should call the member endpoints with the user id', () => {
    service.getMembers(10, 1).subscribe();
    service.inviteMember(10, 1, { email: 'zoe@example.test', role: 'OBSERVER' }).subscribe();
    service.updateRole(10, 100, 1, 'MEMBER').subscribe();

    httpTestingController.expectOne({ method: 'GET', url: `${membersUrl}?userId=1` });
    const invite = httpTestingController.expectOne({ method: 'POST', url: `${membersUrl}?userId=1` });
    expect(invite.request.body).toEqual({ email: 'zoe@example.test', role: 'OBSERVER' });
    const update = httpTestingController.expectOne({ method: 'PUT', url: `${membersUrl}/100?userId=1` });
    expect(update.request.body).toEqual({ role: 'MEMBER' });
  });

  it('should translate invitation errors', () => {
    expect(getInviteErrorMessage(new HttpErrorResponse({ status: 400 }))).toBe('Saisis une adresse e-mail valide et un rôle.');
    expect(getInviteErrorMessage(new HttpErrorResponse({ status: 403 }))).toBe('Seul un administrateur peut inviter des membres.');
    expect(getInviteErrorMessage(new HttpErrorResponse({ status: 409 }))).toBe('Cette personne fait déjà partie du projet.');
    expect(getInviteErrorMessage(new HttpErrorResponse({ status: 500 }))).toBe('Impossible d’envoyer l’invitation.');
    expect(getInviteErrorMessage(new Error('réseau'))).toBe('Impossible d’envoyer l’invitation.');
  });

  it('should translate role errors', () => {
    expect(getRoleErrorMessage(new HttpErrorResponse({ status: 403 }))).toBe('Seul un administrateur peut modifier les rôles.');
    expect(getRoleErrorMessage(new HttpErrorResponse({ status: 409 }))).toBe('Le projet doit garder au moins un administrateur.');
    expect(getRoleErrorMessage(new HttpErrorResponse({ status: 500 }))).toBe('Impossible de modifier le rôle.');
    expect(getRoleErrorMessage(new Error('réseau'))).toBe('Impossible de modifier le rôle.');
  });
});
