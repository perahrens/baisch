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
# Assets are needed by PreloaderBundleGenerator at GWT compile time.
# Strip large unused directories BEFORE compile so they are excluded from
# assets.txt and never downloaded by the browser preloader.
# SpriteSheetCollection (46 MB, 2227 files) and sprites (60 KB) are referenced
# only from dead code (Relicts.java, never instantiated) and tile assets that
# are not used by any active screen.
COPY android/assets/ android/assets/
RUN rm -rf android/assets/data/SpriteSheetCollection android/assets/data/sprites

# Full optimised compile with localWorkers=1 so only one permutation runs at a
# time (keeping peak heap at 1 GB and avoiding OOM on Depot build machines).
# After compilation, assemble the complete serveable directory by overlaying:
#   webapp/  → index.html, styles.css, soundmanager2 files, etc.
#   war/     → assets/ directory built by the PreloaderBundleGenerator
RUN ./gradlew :html:compileGwt --no-daemon --stacktrace && \
    cp -r /workspace/html/webapp/. /workspace/html/build/gwt/out/ && \
    cp -r /workspace/html/war/assets/. /workspace/html/build/gwt/out/

# ── Stage 2: Node.js server ──────────────────────────────────────────────────
FROM node:18-alpine

WORKDIR /app

COPY server/package*.json ./
RUN npm install --omit=dev

COPY server/ ./

# Pull the compiled GWT assets into the public/ directory that express.static
# will serve.
COPY --from=gwt-build /workspace/html/build/gwt/out/ public/

EXPOSE 8080

CMD ["node", "index.js"]
