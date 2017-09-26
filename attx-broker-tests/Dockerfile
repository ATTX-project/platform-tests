FROM frekele/gradle:4.1-jdk8

RUN apt-get update \
    && apt-get install -y wget \
    && apt-get clean

ENV DOCKERIZE_VERSION v0.5.0

RUN wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && rm dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz

RUN mkdir -p /tmp

WORKDIR /tmp

ARG attxTestHelperPlugin
ARG UVreplaceDS

COPY src /tmp/src
COPY build.test.gradle /tmp/build.gradle
COPY runTests.sh /tmp
RUN chmod 700 /tmp/runTests.sh

COPY attx-l-replaceds-${UVreplaceDS}.jar /tmp/attx-l-replaceds-${UVreplaceDS}.jar
COPY dev-test-helper-${attxTestHelperPlugin}.jar /tmp/dev-test-helper.jar
