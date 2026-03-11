// src/app/models/user.ts
export interface User {
  id?: number;      // optionnel à la création
  name: string;     // obligatoire pour register
  email: string;
  password: string;
}