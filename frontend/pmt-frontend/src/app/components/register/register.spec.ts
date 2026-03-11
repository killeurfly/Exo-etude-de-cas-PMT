// src/app/components/register/register.spec.ts
import { RegisterComponent } from './register';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user';
import { of } from 'rxjs';

// Fake AuthService
class FakeAuthService {
  register(user: User) {
    console.log('Fake register appelé avec :', user);
    return of('Inscription OK'); // Simule succès
  }
}

const fakeAuth = new FakeAuthService() as any;
const registerComponent = new RegisterComponent(fakeAuth);

// Test manuel
registerComponent.user = { name: 'Matteo', email: 'test@example.com', password: '1234' };
registerComponent.register();
console.log('Message :', registerComponent.message);