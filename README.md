# PMT — Project Management Tool

Plateforme de gestion de projet collaboratif : comptes, projets, invitations et rôles, tâches (création, assignation, modification, historique), tableau de bord par statut et notification par e-mail lors d'une assignation.

| Partie | Technologies |
|---|---|
| Frontend | Angular 21, Vitest |
| Backend | Java 17, Spring Boot 4, Spring Data JPA, springdoc-openapi |
| Base de données | MySQL 8.4 |
| Industrialisation | Docker, Docker Compose, GitHub Actions, Docker Hub |

## Sommaire

1. [Structure du dépôt](#structure-du-dépôt)
2. [Déploiement avec Docker](#déploiement-avec-docker)
3. [Pipeline CI/CD](#pipeline-cicd)
4. [Tests et couverture](#tests-et-couverture)
5. [Développement sans Docker](#développement-sans-docker)
6. [Configuration](#configuration)
7. [Comptes de démonstration](#comptes-de-démonstration)

## Structure du dépôt

```text
PMT/
├── backend/pmt-backend/      API Spring Boot (+ Dockerfile)
├── frontend/pmt-frontend/    Application Angular (+ Dockerfile, nginx.conf)
├── sql/schema-mysql.sql      Structure de la base et données de démonstration
├── sql/schema-pmt.svg        Schéma relationnel de la base (diagramme vectoriel, + .png)
├── docker-compose.yml        Lancement de l'application complète
├── .env.example              Variables réglables de docker compose
└── .github/workflows/ci.yml  Pipeline CI/CD
```

## Déploiement avec Docker

### Prérequis

- Docker avec Docker Compose v2 (Docker Desktop sous Windows ou macOS).
- Les ports `4200`, `8080` et `8025` libres sur la machine.

### Services lancés

| Service | Image | Rôle | Adresse |
|---|---|---|---|
| `frontend` | `pmt-frontend` (nginx) | Application Angular | http://localhost:4200 |
| `backend` | `pmt-backend` (JRE 17) | API REST | http://localhost:8080 |
| `mysql` | `mysql:8.4` | Base de données (non exposée) | — |
| `mailpit` | `axllent/mailpit` | Serveur SMTP de test qui affiche les e-mails reçus | http://localhost:8025 |

Au premier démarrage, MySQL exécute `sql/schema-mysql.sql` : la base `pmt_db` est créée avec les données de démonstration. Les données sont ensuite conservées dans le volume Docker `mysql-data`. Le backend attend que MySQL soit prêt avant de démarrer.

### Option A : à partir des images publiées sur Docker Hub

Les images sont publiées par la pipeline CI à chaque push (voir [Pipeline CI/CD](#pipeline-cicd)).

1. Créer le fichier `.env` à partir de l'exemple et y indiquer le compte Docker Hub qui publie les images :

   ```bash
   cp .env.example .env
   # puis modifier DOCKERHUB_USERNAME dans .env
   ```

2. Télécharger les images et lancer l'application :

   ```bash
   docker compose pull
   docker compose up -d
   ```

Pour déployer une version précise, indiquer l'identifiant du commit dans `PMT_TAG` (fichier `.env`) au lieu de `latest`.

### Option B : en construisant les images localement

```bash
docker compose up -d --build
```

Les Dockerfiles compilent eux-mêmes le code (Maven pour le backend, npm pour le frontend) : seul Docker est nécessaire.

### Vérifier le déploiement

```bash
docker compose ps        # les 4 services doivent être « running » (mysql « healthy »)
docker compose logs -f backend
```

Puis ouvrir http://localhost:4200 et se connecter avec un [compte de démonstration](#comptes-de-démonstration).

### Arrêter ou réinitialiser

```bash
docker compose down      # arrête et supprime les conteneurs, garde les données
docker compose down -v   # supprime aussi la base : le script SQL sera rejoué au prochain démarrage
```

### Mettre à jour

```bash
docker compose pull      # option A
docker compose up -d     # recrée les conteneurs dont l'image a changé
```

## Pipeline CI/CD

Fichier : [.github/workflows/ci.yml](.github/workflows/ci.yml). La pipeline se déclenche à chaque push sur `main` et `feature/pmt-development`, sur les pull requests vers `main`, ou manuellement depuis l'onglet **Actions**.

| Job | Étapes | Condition |
|---|---|---|
| `frontend` | `npm ci`, tests avec couverture (seuil 60 %), build de production, publication du rapport de couverture | toujours |
| `backend` | `mvnw verify` : tests, contrôle de couverture JaCoCo (seuil 60 %), création du jar ; publication du rapport | toujours |
| `docker` | Construction des deux images et push sur Docker Hub avec les tags `latest` et `<sha du commit>` | push uniquement, après la réussite de `frontend` et `backend` |

### Configuration de Docker Hub (une seule fois)

1. Sur [Docker Hub](https://hub.docker.com), créer un jeton d'accès : **Account settings → Personal access tokens → Generate new token**, avec les droits *Read & Write*.
2. Sur GitHub, dans le dépôt : **Settings → Secrets and variables → Actions → New repository secret**, créer :
   - `DOCKERHUB_USERNAME` : le nom du compte Docker Hub ;
   - `DOCKERHUB_TOKEN` : le jeton créé à l'étape 1.

Les dépôts `pmt-backend` et `pmt-frontend` sont créés automatiquement sur Docker Hub au premier push.

### Rapports de couverture

Dans l'onglet **Actions**, ouvrir une exécution : la section **Artifacts** contient `backend-coverage` et `frontend-coverage` (rapports HTML).

## Tests et couverture

L'objectif est d'au moins 60 % de couverture des instructions et des branches, côté backend et frontend. Les commandes ci-dessous échouent si ce seuil n'est pas atteint.

| Partie | Commande (depuis le dossier de la partie) | Rapport |
|---|---|---|
| Backend | `./mvnw verify` (Windows : `.\mvnw.cmd verify`) | `target/site/jacoco/index.html` |
| Frontend | `npx ng test --watch=false --coverage` | `coverage/pmt-frontend/index.html` |

- Backend : tests d'intégration MockMvc sur base H2 en mémoire (profil `test`) et tests unitaires Mockito.
- Frontend : tests de composants et de services avec Vitest et `HttpTestingController`.

## Développement sans Docker

Prérequis : Java 17, Node.js 22, MySQL 8.4.

1. Créer la base : `mysql -u root < sql/schema-mysql.sql`
2. Backend :
   ```bash
   cd backend/pmt-backend
   ./mvnw spring-boot:run
   ```
3. Frontend :
   ```bash
   cd frontend/pmt-frontend
   npm install
   npm start
   ```

L'application est alors sur http://localhost:4200 et l'API sur http://localhost:8080. Les variables d'environnement du backend sont décrites ci-dessous.

## Configuration

### Variables d'environnement du backend

| Variable | Rôle | Valeur par défaut |
|---|---|---|
| `DB_URL` | URL JDBC de la base | `jdbc:mysql://localhost:3306/pmt_db?useSSL=false&serverTimezone=UTC` |
| `DB_USERNAME` / `DB_PASSWORD` | Identifiants MySQL | `root` / vide |
| `MAIL_HOST` / `MAIL_PORT` | Serveur SMTP | `smtp.gmail.com` / `587` |
| `MAIL_FROM` | Expéditeur des e-mails ; sans lui (ni `MAIL_USERNAME`), aucun e-mail n'est envoyé | vide |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Identifiants SMTP (par exemple Gmail avec un mot de passe d'application) | vide |
| `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` | Authentification et chiffrement SMTP | `true` / `true` |

Dans `docker-compose.yml`, ces variables pointent vers les conteneurs `mysql` et `mailpit`. Aucun secret n'est stocké dans le dépôt.

### Documentation de l'API

Une fois le backend lancé :

- interface Swagger : http://localhost:8080/swagger-ui.html ;
- description OpenAPI (JSON) : http://localhost:8080/v3/api-docs.

L'application n'utilise pas d'authentification sécurisée (non requise par l'énoncé) : chaque appel indique l'utilisateur qui agit avec le paramètre `userId`, obtenu à la connexion.

### Limites connues

- Le frontend appelle l'API sur `http://localhost:8080` et le backend n'accepte que l'origine `http://localhost:4200` : ces ports doivent être conservés.
- Les mots de passe sont stockés en clair, conformément au périmètre pédagogique (sécurisation non requise).

## Comptes de démonstration

| E-mail | Mot de passe | Rôle dans « PMT - Projet de démonstration » |
|---|---|---|
| `alice@example.test` | `alice123` | Administratrice |
| `marc@example.test` | `marc123` | Membre |
| `olivia@example.test` | `olivia123` | Observatrice |

Les e-mails envoyés aux adresses `@example.test` sont visibles dans Mailpit : http://localhost:8025.
