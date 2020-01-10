FROM maven:3.6.0-jdk-12-alpine as build-stage
WORKDIR /function
ENV MAVEN_OPTS -Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyHost= -Dhttps.proxyPort= -Dhttp.nonProxyHosts= -Dmaven.repo.local=/usr/share/maven/ref/repository
ADD pom.xml /function/pom.xml
ADD src /function/src
RUN ["mvn", "package", \
    "dependency:copy-dependencies", \
    "-DincludeScope=runtime", \
    "-Dmdep.prependGroupId=true", \
    "-DoutputDirectory=target" ]

FROM openjdk:12-ea-19-jdk-oraclelinux7
WORKDIR /function

COPY --from=build-stage /function/target/*.jar /function/app/
COPY src/main/c/libfnunixsocket.so /lib

ENTRYPOINT [ "/usr/bin/java", \
    "-XX:+UseSerialGC", \
	 "--enable-preview", \
    "-Xshare:on", \
    "-cp", "/function/app/*", \
    "com.fnproject.fn.runtime.EntryPoint" ]

CMD ["com.example.fn.HelloFunction::handleRequest"]