# ── Stage 1: Build the GWT frontend ──────────────────────────────────────────
FROM eclipse-temurin:11-jdk AS gwt-build

WORKDIR /workspace

# Copy only the files Gradle needs first so layer caching works for dependency
# downloads even when source files change.
COPY gradlew gradlew.bat gradle.properties settings.gradle build.gradle ./
COPY gradle/ gradle/
RUN chmod +x gradlew

# Copy all subproject sources.
COPY core/   core/
COPY html/   html/
# Assets are needed by PreloaderBundleGenerator at GWT compile time
COPY android/assets/ android/assets/

# Draft compile: one JS permutation instead of 5+ → avoids OOM on build machines.
# After compilation, assemble the complete serveable directory by overlaying:
#   webapp/  → index.html, styles.css, soundmanager2 files, etc.
#   war/     → assets/ directory built by the PreloaderBundleGenerator
RUN ./gradlew :html:draftCompileGwt --no-daemon --stacktrace && \
    cp -r /workspace/html/webapp/. /workspace/html/build/gwt/draftOut/ && \
    cp -r /workspace/html/war/. /workspace/html/build/gwt/draftOut/

# ── Stage 2: Node.js server ──────────────────────────────────────────────────
FROM node:18-alpine

WORKDIR /app

COPY server/package*.json ./
RUN npm install --omit=dev

COPY server/ ./

# Pull the compiled GWT assets into the public/ directory that express.static
# will serve.
COPY --from=gwt-build /workspace/html/build/gwt/draftOut/ public/

EXPOSE 8080

CMD ["node", "index.js"]
