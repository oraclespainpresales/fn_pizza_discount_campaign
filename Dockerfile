FROM openjdk:13 as build-stage
WORKDIR /function
RUN curl https://www-eu.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz -o apache-maven-3.6.3-bin.tar.gz 
RUN tar -zxvf apache-maven-3.6.3-bin.tar.gz
RUN yum install -y unzip

COPY dbwallet.zip /function
RUN unzip /function/dbwallet.zip -d /function/wallet/ && rm /function/dbwallet.zip

ENV PATH="/function/apache-maven-3.6.3/bin:${PATH}"

ENV MAVEN_OPTS -Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyHost= -Dhttps.proxyPort= -Dhttp.nonProxyHosts= -Dmaven.repo.local=/usr/share/maven/ref/repository
ADD pom.xml /function/pom.xml
ADD src /function/src
ADD libs/* /function/target/libs/

RUN ["mvn", "install:install-file", "-Dfile=/function/target/libs/ojdbc8.jar",    "-DgroupId=com.oracle.jdbc", "-DartifactId=ojdbc8",    "-Dversion=18.3.0.0", "-Dpackaging=jar"]
RUN ["mvn", "install:install-file", "-Dfile=/function/target/libs/ucp.jar",       "-DgroupId=com.oracle.jdbc", "-DartifactId=ucp",       "-Dversion=18.3.0.0", "-Dpackaging=jar"]
RUN ["mvn", "install:install-file", "-Dfile=/function/target/libs/oraclepki.jar", "-DgroupId=com.oracle.jdbc", "-DartifactId=oraclepki", "-Dversion=18.3.0.0", "-Dpackaging=jar"]
RUN ["mvn", "install:install-file", "-Dfile=/function/target/libs/osdt_core.jar", "-DgroupId=com.oracle.jdbc", "-DartifactId=osdt_core", "-Dversion=18.3.0.0", "-Dpackaging=jar"]
RUN ["mvn", "install:install-file", "-Dfile=/function/target/libs/osdt_cert.jar", "-DgroupId=com.oracle.jdbc", "-DartifactId=osdt_cert", "-Dversion=18.3.0.0", "-Dpackaging=jar"]

#RUN ["mvn", "package"]

RUN ["mvn", "package", \
    "dependency:copy-dependencies", \
    "-DincludeScope=runtime", \
    "-Dmdep.prependGroupId=true", \
    "-DoutputDirectory=target","-e" ]

#RUN /usr/java/openjdk-13/bin/jdeps --print-module-deps --class-path '/function/target/*' /function/target/function.jar
RUN /usr/java/openjdk-13/bin/jlink --no-header-files --no-man-pages --strip-java-debug-attributes --output /function/fnjre --add-modules $(/usr/java/openjdk-13/bin/jdeps --ignore-missing-deps --print-module-deps --class-path '/function/target/*' /function/target/function.jar)

FROM oraclelinux:8-slim
WORKDIR /function

COPY --from=build-stage /function/target/*.jar /function/
COPY --from=build-stage /function/fnjre/ /function/fnjre/
COPY --from=build-stage /function/wallet/ /function/wallet/
COPY libfnunixsocket.so /lib

ENTRYPOINT [ "/function/fnjre/bin/java", \
    "--enable-preview", \
    "-cp", "/function/*", \
    "com.fnproject.fn.runtime.EntryPoint" ]

#ENTRYPOINT [ "/usr/bin/java","-XX:+UseSerialGC","--enable-preview","-Xshare:on","-cp", "/function/*","com.fnproject.fn.runtime.EntryPoint" ]

CMD ["com.example.fn.GetDiscount::handleRequest"]