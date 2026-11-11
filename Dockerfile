# Etapa de compilacion: Maven con Java 21 empaqueta el jar sin ejecutar las pruebas.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# Etapa de ejecucion: solo el JRE y el jar, con un usuario sin privilegios.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home pethaven
COPY --from=build /app/target/*.jar app.jar
USER pethaven

# El contenedor siempre arranca en produccion (sin consola H2, log SQL ni Swagger).
# Variables que el servidor debe definir: JWT_SECRET, CORS_ALLOWED_ORIGINS, ADMIN_EMAIL y ADMIN_PASSWORD.
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
