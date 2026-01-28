FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:1a83d54e00955498af76605d3d58f8764e8e1c3e30d106972c3b895f76263c33

ENV TZ="Europe/Oslo"
ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.datadeling.AppKt"]