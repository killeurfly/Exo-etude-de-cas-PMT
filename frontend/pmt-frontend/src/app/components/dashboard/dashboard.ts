import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CreateProjectRequest, Project } from '../../models/project';
import { AuthService } from '../../services/auth.service';
import { getProjectErrorMessage, ProjectService } from '../../services/project.service';

/**
 * Tableau de bord : liste des projets de l'utilisateur et formulaire de création d'un projet.
 */
@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  readonly projects = signal<Project[]>([]);
  readonly message = signal('');
  readonly loading = signal(false);
  readonly saving = signal(false);
  form: CreateProjectRequest = { name: '', description: '', startDate: '' };

  constructor(
    readonly authService: AuthService,
    private readonly projectService: ProjectService,
  ) {}

  ngOnInit(): void {
    const user = this.authService.currentUser();
    if (user === null) {
      this.message.set('Connecte-toi pour consulter tes projets.');
      return;
    }

    this.loadProjects(user.id);
  }

  /** Crée le projet saisi et l'ajoute en tête de liste. */
  createProject(): void {
    const user = this.authService.currentUser();
    if (user === null) {
      this.message.set('Connecte-toi pour créer un projet.');
      return;
    }

    this.message.set('');
    this.saving.set(true);
    this.projectService.createProject(user.id, this.form).subscribe({
      next: (project) => {
        this.projects.update((projects) => [project, ...projects]);
        this.form = { name: '', description: '', startDate: '' };
        this.saving.set(false);
        this.message.set('Projet créé. Tu en es administrateur.');
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.message.set(getProjectErrorMessage(error, 'Impossible de créer le projet.'));
      },
    });
  }

  /** Charge les projets de l'utilisateur. */
  private loadProjects(userId: number): void {
    this.loading.set(true);
    this.projectService.getProjects(userId).subscribe({
      next: (projects) => {
        this.projects.set(projects);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.message.set(getProjectErrorMessage(error, 'Impossible de charger les projets.'));
      },
    });
  }
}
