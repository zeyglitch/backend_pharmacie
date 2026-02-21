# ============================================
# Stage 1 : Build avec Maven
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copier d'abord les fichiers de configuration Maven pour profiter du cache Docker
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

# Télécharger les dépendances (couche cachée si pom.xml ne change pas)
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Compiler le projet (sans exécuter les tests)
RUN mvn package -DskipTests -B

# ============================================
# Stage 2 : Image de production légère
# ============================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copier le JAR depuis le stage de build
COPY --from=build /app/target/*.jar app.jar

# Créer le dossier pour les images uploadées
RUN mkdir -p /tmp/images

# Port exposé (Render utilise la variable PORT)
EXPOSE 8080

# Variables d'environnement par défaut
# Sur Render, ces valeurs seront surchargées par les Environment Variables du dashboard
ENV SPRING_PROFILES_ACTIVE=deploy
ENV SERVER_PORT=8080

# Point d'entrée (shell form pour expansion de $PORT)
CMD java -jar app.jar --server.port=${PORT:-8080}
