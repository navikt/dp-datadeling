FROM ghcr.io/navikt/baseimages/temurin:17

COPY build/install/* /
EXPOSE 8080
