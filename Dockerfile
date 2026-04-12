# syntax=docker/dockerfile:1
# ── Stage 1: Build the GWT frontend ──────────────────────────────────────────
FROM eclipse-temurin:11-jdk AS gwt-build

WORKDIR /workspace

# Copy only the build descriptor files first.  This layer is cached until those
# files change, allowing the dependency-download step below to be skipped on
# most deploys.
COPY gradlew gradlew.bat gradle.properties settings.gradle build.gradle ./
COPY gradle/ gradle/
COPY core/build.gradle  core/build.gradle
COPY html/build.gradle  html/build.gradle
RUN chmod +x gradlew

# Resolve and cache all Gradle/GWT dependencies without compiling source.
# The --mount=type=cache keeps the Gradle home across builds on the same
# Depot builder, so JARs are not re-downloaded on every source change.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :html:dependencies --no-daemon -q

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

# Full optimised compile.  The Gradle home cache is shared with the dependency
# step so previously downloaded JARs are still present.
# The GWT work dir cache preserves the unit cache between Docker builds,
# enabling incremental compilation when only a few source files changed
# (typically 3-5x faster than a cold compile).
# After compilation, assemble the complete serveable directory by overlaying:
#   webapp/  → index.html, styles.css, soundmanager2 files, etc.
#   war/assets/ → assets/ dir built by PreloaderBundleGenerator, served at
#                 /assets/ so the GWT preloader (baseURL + "assets/") can find
#                 assets.txt and all game files.
# The sounds/ subdirectory is NOT managed by PreloaderBundleGenerator; we copy
# it explicitly from android/assets so all audio files are always present.
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/workspace/html/build/gwt/work \
    ./gradlew :html:compileGwt --no-daemon --stacktrace && \
    mkdir -p /workspace/html/war/assets/data/sounds && \
    cp /workspace/android/assets/data/sounds/*.mp3 /workspace/html/war/assets/data/sounds/ && \
    cp -r /workspace/html/webapp/. /workspace/html/build/gwt/out/ && \
    cp -r /workspace/html/war/assets /workspace/html/build/gwt/out/assets

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
