# 🔐 ICP-Brasil Authenticator

Esta é uma aplicação Spring Boot projetada para autenticar usuários utilizando certificados digitais do padrão ICP-Brasil através de autenticação mútua SSL/TLS (mTLS). A API valida o certificado do cliente, extrai informações detalhadas para auditoria e as retorna em formato JSON.

## ✨ Funcionalidades

- **Autenticação mTLS**: Valida certificados de cliente X.509.
- **Padrão ICP-Brasil**: Configurada com a cadeia de confiança completa da ICP-Brasil.
- **Auditoria Completa**: Extrai e loga dados essenciais para auditoria, como Serial Number, Fingerprint SHA-256, CPF, Subject/Issuer DN, etc.
- **API REST**: Expõe um endpoint simples para validação.
- **Container-Ready**: Inclui `Dockerfile` e `docker-compose.yml` para fácil deploy.
- **Página de Teste**: Contém um `index.html` para testar a autenticação diretamente no navegador.

---

## 🚀 Guia de Configuração e Execução

Para executar a aplicação, é crucial configurar corretamente os certificados digitais: o **Keystore** (identidade do servidor) e o **Truststore** (autoridades certificadoras confiáveis).

### 📋 Pré-requisitos

- Java 21+
- Maven 3.8+
- Docker e Docker Compose (para execução em container)
- OpenSSL ou `keytool` (ferramenta de linha de comando do Java)

---

### 🔑 Passo a Passo: Configurando os Certificados

Os arquivos de certificado (`keystore.p12` e `truststore.p12`) devem ser colocados na pasta `src/main/resources`.

#### 1. Obter a Cadeia de Certificados da ICP-Brasil

O `truststore` deve conter todas as Autoridades Certificadoras (ACs) da ICP-Brasil para que o servidor possa confiar nos certificados de usuário emitidos por elas.

1.  **Acesse o repositório oficial do ITI (Instituto Nacional de Tecnologia da Informação):**
    [Certificados das ACs da ICP-Brasil (Arquivo Único Compactado)](https://www.gov.br/iti/pt-br/assuntos/repositorio/certificados-das-acs-da-icp-brasil-arquivo-unico-compactado)

2.  **Baixe o arquivo ZIP** e descompacte-o em uma pasta temporária. Por exemplo, `~/temp/icp-brasil-certs`. Dentro dela, você encontrará a pasta `Accompactado` com centenas de arquivos `.crt`.

#### 2. Criar o `truststore.p12`

Este arquivo irá agrupar todos os certificados `.crt` da ICP-Brasil.

- **Local de destino**: `src/main/resources/truststore.p12`

Execute o comando abaixo no seu terminal, dentro da pasta onde você descompactou os certificados. Ele irá iterar por todos os arquivos `.crt` e importá-los para um novo `truststore.p12`.

```bash
# Navegue até a pasta que contém os certificados .crt
cd ~/temp/icp-brasil-certs/Accompactado

# Defina uma senha para o seu truststore
export TRUSTSTORE_PASS="sua_senha_segura_aqui"

# Comando para importar todos os certificados .crt
for cert in *.crt; do
  alias=$(openssl x509 -in "$cert" -noout -subject | sed -n 's/.*CN=\([^,]*\).*/\1/p' | tr -d '[:space:]')
  if [ -z "$alias" ]; then
    alias="$cert"
  fi
  keytool -import -trustcacerts -file "$cert" \
          -alias "$alias" \
          -keystore ../../truststore.p12 \
          -storepass "$TRUSTSTORE_PASS" \
          -noprompt
done

# Mova o truststore gerado para o local correto no projeto
mv ../../truststore.p12 /caminho/para/seu/projeto/icp-brasil-authenticator/src/main/resources/
```

**Atenção**: Substitua `sua_senha_segura_aqui` por uma senha forte e anote-a. Você a usará na configuração da aplicação.

#### 3. Criar o `keystore.p12` (Certificado do Servidor)

Este arquivo contém a chave privada e o certificado público do seu servidor, permitindo que ele se identifique para os clientes (navegadores) via HTTPS.

- **Local de destino**: `src/main/resources/keystore.p12`

Para **desenvolvimento local**, você pode criar um certificado autoassinado:

```bash
# Defina uma senha para o seu keystore
export KEYSTORE_PASS="outra_senha_segura_aqui"

# Comando para gerar o keystore com um certificado autoassinado para localhost
keytool -genkeypair -alias selfsigned \
        -keyalg RSA -keysize 2048 \
        -validity 365 \
        -keystore src/main/resources/keystore.p12 \
        -storepass "$KEYSTORE_PASS" \
        -keypass "$KEYSTORE_PASS" \
        -dname "CN=localhost, OU=Dev, O=TCESP, L=Sao Paulo, ST=SP, C=BR" \
        -storetype PKCS12
```

**Atenção**: Para **produção**, substitua este keystore por um que contenha um certificado SSL válido emitido por uma autoridade certificadora reconhecida.

#### 4. Configurar as Senhas na Aplicação

As senhas que você definiu nos passos anteriores devem ser fornecidas à aplicação como variáveis de ambiente.

Edite o arquivo `docker-compose.yml` (ou seu script de inicialização) para incluir as senhas:

```yaml
# Em docker-compose.yml
services:
  icp-brasil-authenticator:
    # ...
    environment:
      - KEYSTORE_PASSWORD=outra_senha_segura_aqui
      - TRUSTSTORE_PASSWORD=sua_senha_segura_aqui
```

---

## 🏃‍♀️ Executando a Aplicação

### Com Maven (Localmente)

1.  Certifique-se de que os arquivos `keystore.p12` e `truststore.p12` estão em `src/main/resources`.
2.  Exporte as variáveis de ambiente com as senhas no seu terminal.
3.  Execute o comando:
    ```bash
    mvn spring-boot:run
    ```

### Com Docker

1.  Construa a imagem Docker:
    ```bash
    docker build -t icp-brasil-authenticator:latest .
    ```
2.  Inicie o container usando Docker Compose:
    ```bash
    docker-compose up -d
    ```

A aplicação estará disponível em `https://localhost:8443`.

---

## 📡 Endpoint da API

- **URL**: `GET /api/certificate/validate`
- **Descrição**: Endpoint principal para autenticação. O navegador ou cliente deve apresentar um certificado de cliente válido durante o handshake SSL/TLS.
- **Resposta de Sucesso (200 OK)**: Um JSON contendo os dados extraídos do certificado para fins de auditoria.
- **Resposta de Erro (400 Bad Request)**: Ocorre se nenhum certificado de cliente for apresentado.

## 🛡️ Segurança

- Os arquivos `*.p12` são sensíveis e **NUNCA** devem ser commitados no repositório Git. O arquivo `.gitignore` já está configurado para ignorá-los.
- Use senhas fortes para o keystore e o truststore e gerencie-as de forma segura (ex: secrets do Docker/Kubernetes, Vault).
- Em produção, utilize um certificado SSL válido no keystore.
- O `Dockerfile` cria um usuário não-root (`appuser`) para executar a aplicação, seguindo as melhores práticas de segurança.
