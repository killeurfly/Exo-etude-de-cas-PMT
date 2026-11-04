CREATE DATABASE IF NOT EXISTS pmt_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE pmt_db;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(254) NOT NULL,
  password VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS projects (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(160) NOT NULL,
  description TEXT NOT NULL,
  start_date DATE NOT NULL,
  created_by_user_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_projects_creator (created_by_user_id),
  CONSTRAINT fk_projects_creator
    FOREIGN KEY (created_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS project_members (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  email VARCHAR(254) NOT NULL,
  role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'INVITED',
  invited_by_user_id BIGINT NULL,
  joined_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_project_members_project_email (project_id, email),
  UNIQUE KEY uq_project_members_project_user (project_id, user_id),
  KEY idx_project_members_user (user_id),
  CONSTRAINT chk_project_members_active_user
    CHECK (status <> 'ACTIVE' OR user_id IS NOT NULL),
  CONSTRAINT chk_project_members_role
    CHECK (role IN ('ADMIN', 'MEMBER', 'OBSERVER')),
  CONSTRAINT chk_project_members_status
    CHECK (status IN ('INVITED', 'ACTIVE')),
  CONSTRAINT fk_project_members_project
    FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
  CONSTRAINT fk_project_members_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_project_members_invited_by
    FOREIGN KEY (invited_by_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tasks (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  name VARCHAR(180) NOT NULL,
  description TEXT NOT NULL,
  due_date DATE NULL,
  priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
  status VARCHAR(20) NOT NULL DEFAULT 'TODO',
  completed_at TIMESTAMP NULL,
  created_by_user_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_tasks_id_project (id, project_id),
  KEY idx_tasks_project_status (project_id, status),
  KEY idx_tasks_due_date (due_date),
  CONSTRAINT chk_tasks_priority
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
  CONSTRAINT chk_tasks_status
    CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
  CONSTRAINT fk_tasks_project
    FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
  CONSTRAINT fk_tasks_creator
    FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task_assignments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  assigned_by_user_id BIGINT NULL,
  assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_task_assignments_task_user (task_id, user_id),
  KEY idx_task_assignments_project_user (project_id, user_id),
  CONSTRAINT fk_task_assignments_task
    FOREIGN KEY (task_id, project_id) REFERENCES tasks (id, project_id) ON DELETE CASCADE,
  CONSTRAINT fk_task_assignments_member
    FOREIGN KEY (project_id, user_id)
    REFERENCES project_members (project_id, user_id) ON DELETE CASCADE,
  CONSTRAINT fk_task_assignments_assigner
    FOREIGN KEY (assigned_by_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  changed_by_user_id BIGINT NULL,
  field_name VARCHAR(80) NOT NULL,
  old_value TEXT NULL,
  new_value TEXT NULL,
  changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_task_history_task_date (task_id, changed_at),
  CONSTRAINT fk_task_history_task
    FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
  CONSTRAINT fk_task_history_user
    FOREIGN KEY (changed_by_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Demo accounts and passwords for local testing.
INSERT INTO users (name, email, password)
VALUES
  ('Alice Admin', 'alice@example.test', 'alice123'),
  ('Marc Member', 'marc@example.test', 'marc123'),
  ('Olivia Observer', 'olivia@example.test', 'olivia123')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  password = VALUES(password);

SET @alice_id = (SELECT id FROM users WHERE email = 'alice@example.test');
SET @marc_id = (SELECT id FROM users WHERE email = 'marc@example.test');
SET @olivia_id = (SELECT id FROM users WHERE email = 'olivia@example.test');

INSERT INTO projects (name, description, start_date, created_by_user_id)
SELECT 'PMT - Projet de démonstration',
       'Projet de démonstration pour valider les projets, les rôles et les tâches.',
       '2026-01-15',
       @alice_id
WHERE NOT EXISTS (
  SELECT 1
  FROM projects
  WHERE name = 'PMT - Projet de démonstration'
    AND created_by_user_id = @alice_id
);

SET @project_id = (
  SELECT id
  FROM projects
  WHERE name = 'PMT - Projet de démonstration'
    AND created_by_user_id = @alice_id
  ORDER BY id
  LIMIT 1
);

INSERT INTO project_members (project_id, user_id, email, role, status, invited_by_user_id, joined_at)
SELECT @project_id, @alice_id, 'alice@example.test', 'ADMIN', 'ACTIVE', @alice_id, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
  SELECT 1 FROM project_members
  WHERE project_id = @project_id AND email = 'alice@example.test'
);

INSERT INTO project_members (project_id, user_id, email, role, status, invited_by_user_id, joined_at)
SELECT @project_id, @marc_id, 'marc@example.test', 'MEMBER', 'ACTIVE', @alice_id, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
  SELECT 1 FROM project_members
  WHERE project_id = @project_id AND email = 'marc@example.test'
);

INSERT INTO project_members (project_id, user_id, email, role, status, invited_by_user_id, joined_at)
SELECT @project_id, @olivia_id, 'olivia@example.test', 'OBSERVER', 'ACTIVE', @alice_id, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
  SELECT 1 FROM project_members
  WHERE project_id = @project_id AND email = 'olivia@example.test'
);

INSERT INTO tasks (project_id, name, description, due_date, priority, status, created_by_user_id)
SELECT @project_id, 'Concevoir le modèle de données',
       'Définir les entités et les relations du projet.',
       '2026-11-15', 'HIGH', 'IN_PROGRESS', @alice_id
WHERE NOT EXISTS (
  SELECT 1 FROM tasks
  WHERE project_id = @project_id AND name = 'Concevoir le modèle de données'
);

INSERT INTO tasks (project_id, name, description, due_date, priority, status, created_by_user_id)
SELECT @project_id, 'Créer les écrans du tableau de bord',
       'Afficher les tâches regroupées par statut.',
       '2026-12-01', 'MEDIUM', 'TODO', @alice_id
WHERE NOT EXISTS (
  SELECT 1 FROM tasks
  WHERE project_id = @project_id AND name = 'Créer les écrans du tableau de bord'
);

SET @task_data_model_id = (
  SELECT id FROM tasks
  WHERE project_id = @project_id AND name = 'Concevoir le modèle de données'
  ORDER BY id LIMIT 1
);

INSERT INTO tasks (
  project_id, name, description, due_date, priority, status, completed_at, created_by_user_id
)
SELECT @project_id, 'Initialiser le dépôt GitHub',
       'Préparer le dépôt et la branche de travail.',
       '2026-11-01', 'LOW', 'DONE', '2026-10-30 16:00:00', @alice_id
WHERE NOT EXISTS (
  SELECT 1 FROM tasks
  WHERE project_id = @project_id AND name = 'Initialiser le dépôt GitHub'
);

INSERT INTO task_assignments (project_id, task_id, user_id, assigned_by_user_id)
SELECT @project_id, @task_data_model_id, @marc_id, @alice_id
WHERE NOT EXISTS (
  SELECT 1 FROM task_assignments
  WHERE task_id = @task_data_model_id AND user_id = @marc_id
);

INSERT INTO task_history (task_id, changed_by_user_id, field_name, old_value, new_value)
SELECT @task_data_model_id, @alice_id, 'status', 'TODO', 'IN_PROGRESS'
WHERE NOT EXISTS (
  SELECT 1 FROM task_history
  WHERE task_id = @task_data_model_id
    AND field_name = 'status'
    AND old_value = 'TODO'
    AND new_value = 'IN_PROGRESS'
);
