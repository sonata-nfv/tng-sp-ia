FROM maven:3.5-jdk-10-slim

MAINTAINER D.Valocchi <d.valocchi@ucl.ac.uk>

ADD adaptor /adaptor

WORKDIR /adaptor

RUN mvn -e -q compile assembly:single;

# New small container with java 10
FROM openjdk:10-jre-slim

RUN apt-get update && apt-get install --no-install-recommends -y \
   netcat && rm -rf /var/lib/apt/lists/*

ADD adaptor /adaptor
ADD broker.config /etc/son-mano/broker.config
ADD postgres.config /etc/son-mano/postgres.config
ADD sonata.config /etc/son-mano/sonata.config

ADD son-sp-infra-* /usr/local/bin/
ADD son-sp-infra /etc/init.d/
ADD ./setenv.sh \
   ./docker-entrypoint.sh /

RUN chmod +x /setenv.sh \
   /docker-entrypoint.sh \
   /usr/local/bin/son-sp-infra-* \
   /etc/init.d/son-sp-infra

WORKDIR /adaptor

ENV broker_host broker
ENV broker_port 5672
ENV broker_exchange son-kernel
ENV broker_uri amqp://guest:guest@broker:5672/%2F

ENV repo_host postgres
ENV repo_port 5432
ENV repo_user sonatatest
ENV repo_pass sonata
ENV SONATA_SP_ADDRESS 127.0.0.1

# Copy the artifact generated on first container to this one
COPY --from=0 /adaptor/target/adaptor-0.0.1-SNAPSHOT-jar-with-dependencies.jar /adaptor/target/


CMD ["/docker-entrypoint.sh"]


