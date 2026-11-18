import { Routes } from '@angular/router';
import { Dashboard } from './components/dashboard/dashboard';
import { LoginComponent } from './components/login/login';
import { ProjectDetail } from './components/project/project';
import { RegisterComponent } from './components/register/register';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'dashboard', component: Dashboard },
  { path: 'projects/:id', component: ProjectDetail },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' },
];