FROM ghcr.io/navikt/baseimages/temurin:17

COPY build/libs/dp-datadeling-all.jar /app/app.jar
EXPOSE 8080
