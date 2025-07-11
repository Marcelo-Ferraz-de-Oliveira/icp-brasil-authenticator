# Dockerfile para ICP-Brasil Authenticator
# Build em 2 estágios conforme padrão TCESP

# ====================================
# ESTÁGIO 1: COMPILAÇÃO
# ====================================
FROM maven:3.8.8-eclipse-temurin-21 AS compile

# Variáveis de ambiente para Maven
ARG MAVEN_OPTS
ARG JAVA_OPTS

# Configurar variáveis de ambiente
ENV MAVEN_OPTS=${MAVEN_OPTS}
ENV JAVA_OPTS=${JAVA_OPTS}

# Definir diretório de trabalho
WORKDIR /app

# Copiar arquivos de configuração do Maven primeiro (cache layer)
COPY pom.xml .
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn

# Baixar dependências (aproveitamento de cache do Docker)
RUN mvn dependency:go-offline -B

# Copiar código fonte
COPY src ./src

# Compilar aplicação
RUN mvn clean package -DskipTests -B

# ====================================
# ESTÁGIO 2: RUNTIME
# ====================================
FROM eclipse-temurin:21-jre AS runtime

# Argumentos para configurações Java
ARG JAVA_OPTS
ARG MAVEN_OPTS

# Variáveis de ambiente
ENV JAVA_OPTS=${JAVA_OPTS}
ENV MAVEN_OPTS=${MAVEN_OPTS}

# Criar usuário não-root para segurança
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Criar diretório da aplicação
WORKDIR /app

# Copiar JAR da aplicação do estágio de compilação
COPY --from=compile /app/target/*.jar app.jar

# Copiar certificados e keystores se necessário (opcional)
COPY --from=compile /app/src/main/resources/*.p12 ./

# Alterar proprietário dos arquivos
RUN chown -R appuser:appuser /app

# Mudar para usuário não-root
USER appuser

# Expor porta da aplicação
EXPOSE 8443

# Comando de execução
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
