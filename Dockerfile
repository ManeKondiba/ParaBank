# Multi-stage Dockerfile for Ephemeral ParaBank Banking Environment
# Pinned to revision 98c1c9ab4889eb92c7798da63bb417e27261d3d5 on Temurin JDK 21 & Tomcat 11

FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

RUN apt-get update && apt-get install -y curl unzip && rm -rf /var/lib/apt/lists/*

ARG PARABANK_REV=98c1c9ab4889eb92c7798da63bb417e27261d3d5
RUN curl -fsSL "https://codeload.github.com/parasoft/parabank/zip/${PARABANK_REV}" -o parabank.zip && \
    unzip -q parabank.zip && \
    mv "parabank-${PARABANK_REV}" src-parabank

WORKDIR /build/src-parabank
RUN mvn clean package -DskipTests -B

# Runtime stage: Tomcat 11 on JDK 21
FROM tomcat:11.0-jdk21-temurin
LABEL maintainer="ParaBank Automation Team"

# Remove default Tomcat webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Deploy ParaBank WAR
COPY --from=builder /build/src-parabank/target/parabank-*.war /usr/local/tomcat/webapps/parabank.war

# Default HTTP port
EXPOSE 8080

HEALTHCHECK --interval=5s --timeout=3s --retries=15 \
  CMD curl -f http://localhost:8080/parabank/services/bank/openapi.json || exit 1

CMD ["catalina.sh", "run"]
