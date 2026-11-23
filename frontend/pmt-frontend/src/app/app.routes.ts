import { Routes } from '@angular/router';
import { Dashboard } from './components/dashboard/dashboard';
import { LoginComponent } from './components/login/login';
import { ProjectDetail } from './components/project/project';
import { RegisterComponent } from './components/register/register';
import { TaskDetail } from './components/task/task';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'dashboard', component: Dashboard },
  { path: 'projects/:id', component: ProjectDetail },
  { path: 'projects/:id', component: ProjectDetail },
  { path: 'projects/:id/tasks/:taskId', component: TaskDetail },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' },
];