# syntax=docker/dockerfile:1
# Stage "build": compile + unit tests.  Stage "artifact": just the jar (docker build --target artifact -o out .).
# Stage "server": Paper server with the plugin baked in, for the bot test in docker-compose.yml.

ARG MC_IMAGE=itzg/minecraft-server:java21

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY core core
COPY plugin plugin
COPY bungee bungee
COPY velocity velocity
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package

FROM scratch AS artifact
COPY --from=build /src/plugin/target/PersianFontFixer.jar /PersianFontFixer.jar
COPY --from=build /src/bungee/target/PersianFontFixer-bungee.jar /PersianFontFixer-bungee.jar
COPY --from=build /src/velocity/target/PersianFontFixer-velocity.jar /PersianFontFixer-velocity.jar

FROM ${MC_IMAGE} AS server
COPY --from=build /src/plugin/target/PersianFontFixer.jar /plugins/PersianFontFixer.jar

# Proxy variants for the end-to-end test; the backend behind them runs without the plugin.
FROM itzg/bungeecord AS proxy-bungee
COPY --from=build /src/bungee/target/PersianFontFixer-bungee.jar /plugins/PersianFontFixer-bungee.jar
COPY docker/proxy/bungee/config.yml /config/config.yml

FROM itzg/bungeecord AS proxy-velocity
COPY --from=build /src/velocity/target/PersianFontFixer-velocity.jar /plugins/PersianFontFixer-velocity.jar
COPY docker/proxy/velocity/velocity.toml /config/velocity.toml
