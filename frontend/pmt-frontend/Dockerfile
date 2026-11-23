# Étape 1 : compilation de l'application Angular.
FROM node:22-alpine AS build
WORKDIR /app

# Les dépendances sont installées avant de copier le code : tant que package-lock.json ne change pas,
# Docker réutilise cette couche en cache.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Étape 2 : fichiers statiques servis par nginx.
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/pmt-frontend/browser /usr/share/nginx/html

EXPOSE 80
