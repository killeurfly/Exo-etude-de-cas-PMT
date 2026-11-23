# Étape 1 : compilation du jar avec le JDK et le wrapper Maven du projet.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Les dépendances sont téléchargées avant de copier le code : tant que le pom.xml ne change pas,
# Docker réutilise cette couche en cache et la reconstruction est beaucoup plus rapide.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
# Les tests sont exécutés par la CI avant la construction de l'image.
RUN ./mvnw -B package -DskipTests

# Étape 2 : image d'exécution, avec seulement le JRE et le jar.
FROM eclipse-temurin:17-jre
WORKDIR /app

# L'application ne tourne pas en root.
RUN groupadd --system pmt && useradd --system --gid pmt pmt
COPY --from=build /app/target/pmt-backend-*.jar app.jar
USER pmt

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
