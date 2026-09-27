# --- Development Stage ---
FROM eclipse-temurin:26-jdk-alpine AS dev

WORKDIR /app

# Die Wrapper-Dateien kopieren
COPY gradlew gradlew
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Hier zwingen wir Gradle, das lokale JDK des Containers für die Toolchain zu registrieren
ENV GRADLE_OPTS="-Dorg.gradle.java.installations.auto-detect=false -Dorg.gradle.java.installations.paths=/opt/java/openjdk"

# Abhängigkeiten cachen
RUN ./gradlew dependencies --no-daemon || true

# Quellcode kopieren
COPY src ./src

EXPOSE 8080 35729 5005

# Startbefehl inklusive der Pfad-Anweisung für die Toolchain
CMD ["./gradlew", "bootRun", "--no-daemon", "-Dorg.gradle.java.installations.auto-detect=false", "-Dorg.gradle.java.installations.paths=/opt/java/openjdk", "-PjvmArgs=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"]