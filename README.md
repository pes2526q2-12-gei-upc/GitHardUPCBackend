# GitHardUPC Backend

Aquest és el repositori del backend del projecte **GitHardUPC**. A continuació trobareu les instruccions detallades per a la configuració i instal·lació del projecte en el vostre entorn local.

## Membres:

* Mallofré Grau, Gerard
* Nebot Riera, Pol
* Berruezo Vazquez, Oriol
* Gorro Duran, Martí
* López Andreu, Oscar
* Munné Carbonell, Joel
* Fiori Porta, Marc

---

## Requisits Previs

Abans de començar, assegureu-vos de tenir instal·lat:
* **Java 21**
* **Maven**
* **Python 3.x** (amb entorn virtual `.venv`)
* **Docker** i **Docker Compose**
* **PostgreSQL** (si no s'utilitza Docker per a la BD)

---

## Configuració del Projecte

Seguiu aquests passos per configurar l'entorn de desenvolupament:

### 1. Configuració de Firebase
Heu de col·locar el fitxer de credencials de Firebase (`.json`) a la carpeta de configuració:
* **Ruta:** `config/firebase-config.json`

Afegiu la ruta del fitxer de credencials de Firebase a `application-local.properties`:

```properties
app.firebase.config=config/firebase-config.json
```

O configureu-la com a variable d'entorn:

```powershell
# Windows
setx FIREBASE_CONFIG_PATH "C:/ruta/al/fitxer/firebase-config.json"
```

### 2. Propietats Locals (`application-local.properties`)
Heu de crear un fitxer de configuració local per sobreescriure les propietats per defecte:
1. Copieu el fitxer `src/main/resources/application-local.properties.example`.
2. Enganxeu-lo a la mateixa carpeta i anomeneu-lo `application-local.properties`.
3. Configureu les següents dades segons el vostre entorn:

#### Base de Dades (BD)
Actualitzeu la URL, l'usuari i la contrasenya de la vostra base de dades:
```properties
spring.datasource.url=jdbc:postgresql://vostra_url:port/nom_bd
spring.datasource.username=el_vostre_usuari
```

#### Contrasenya d'Administrador i Tokens
Configureu la contrasenya del panell d'administració i el token de l'API externa:

```properties
admin.password=la_vostra_contrasenya_admin
api.external.token=el_vostre_token_extern
```

### 3. Variables d'Entorn
Per seguretat, la contrasenya de la base de dades s'ha de configurar com una variable d'entorn anomenada `SAFESTEPS_DB_PASSWORD`.

* **Windows (PowerShell):**
  ```powershell
  setx SAFESTEPS_DB_PASSWORD "la_vostra_contrasenya"
  ```

* **IntelliJ IDEA:**
  `Run` -> `Edit Configurations` -> `Environment Variables` -> Afegir `SAFESTEPS_DB_PASSWORD=la_vostra_contrasenya`.

### 4. Configuració de Scripts i Python
Assegureu-vos que les rutes dels scripts i de l'executable de Python siguin correctes en el vostre `application-local.properties` o `application.properties`:

```properties
backend.scheduler.python.command=python
backend.scheduler.scripts.path=src/scripts/
```

> [!IMPORTANT]
> Comproveu que el comando `python` és el correcte en el vostre sistema. Per exemple, a Windows es pot utilitzar `python3` o la ruta directa a l'executable de python del sistema o d'un entorn virtual.

---

## Configuració de Continuous Integration / Deployment (CI/CD)

El projecte està configurat amb **GitHub Actions** per a CI/CD automàtic. Els fitxers de configuració es troben a `.github/workflows/`:

### Workflows Configurats

#### 1. **CI (Integració Contínua)** - `backend-ci.yml`
S'executa automàticament en cada **push** o **pull request** a les branques: `main`, `develop`, `config/Integracio_Continua`

**Passos:**
- Checkout del codi
- Instal·lació de JDK 21
- Compilació i execució de tests (`./mvnw clean verify`)
- Anàlisi de codi amb SonarQube

#### 2. **CD (Continuous Deployment)** - `backend-cd.yml`
S'executa **automàticament** quan el CI passa correctament (build verd) a les branques: `main` o `develop`

**Passos:**
- Compilació de l'aplicació (`./mvnw package`)
- Transferència del JAR al servidor via SCP
- Desplegament automàtic del JAR al servidor
- Versionatge automàtic:
    - **main**: Versió major (v1.0, v2.0, etc.) al port **8081** (producció)
    - **develop**: Versió minor (v1.1, v1.2, etc.) al port **8082** (staging)
- Registre de logs per versió

### Configuració de Secrets de GitHub

Per fer funcionar el CD s'han de configurar els secrets següents a l'apartat **Settings** > **Secrets and Variables** > **Actions**:

| Secret | Descripció                                         | Exemple                        |
|--------|----------------------------------------------------|--------------------------------|
| `SERVER_HOST` | Adreça IP o hostname del servidor                  | `192.168.1.100` o `servidor.com` |
| `SERVER_USER` | Usuari del servidor                                | `user`                         |
| `SSH_PRIVATE_KEY` | Clau privada SSH                                   | CLAUSSHMOLTPRIVADA             |
| `SAFESTEPS_DB_PASSWORD` | Contrasenya de la base de dades PostgreSQL, CI i CD | `your_secure_password`         |
| `SONAR_TOKEN` | Token de SonarQube per a anàlisi de codi           | Token generat a SonarQube      |

> [!WARNING]
> **No pugeu secrets reals al vostre repositori!** Els secrets de GitHub s'emmagatzemen de forma segura.

#### Generar clau SSH per al Secret
```bash
# Generar parella de claus de seguretat de forma silenciosa
ssh-keygen -q -t rsa -b 4096 -N "" -f ~/.ssh/deploy_rsa

cat ~/.ssh/deploy_rsa       # Copiar tot el contingut com a `SSH_PRIVATE_KEY`
cat ~/.ssh/deploy_rsa.pub   # Afegir a ~/.ssh/authorized_keys del servidor
```

---

## Docker

Si voleu aixecar els serveis (com la base de dades) utilitzant Docker:

1. Baixeu i instal·leu Docker Desktop.
2. Reinicieu el PC i executeu Docker Desktop.
3. Comproveu que està en "Engine running".
4. Executeu la següent comanda a l'arrel del projecte:

```bash
docker compose up -d
```

---

## Execució de l'Aplicació

Un cop configurat tout, podeu executar l'aplicació amb Maven (en local) o amb els scripts preparats.

### Execució local

#### A Windows (PowerShell):
```powershell
./mvnw spring-boot:run
```

#### A Linux / MacOS:
```bash
./mvnw spring-boot:run
```

O podeu utilitzar el vostre IDE (IntelliJ, Eclipse, etc.) executant la classe principal `BackendApplication`.

L'aplicació s'iniciarà al port configurat (per defecte `8080` si no està específicat a `application-local.properties`).

### Altres comandes útils:

#### Compilar el projecte:
```powershell
./mvnw clean install
```

#### Empaquetar el projecte (generar JAR):
```powershell
./mvnw package
```

#### Executar tests:
```powershell
./mvnw test
```

#### Execució de tests complets (unit + integration):
```powershell
./mvnw clean verify
```

#### Reiniciar la base de dades (Docker):
```bash
docker compose down -v
# o si useu la versió antiga:
docker-compose down -v

docker compose up -d
# o si useu la versió antiga:
docker-compose up -d
```

#### Veure logs de Docker:
```bash
docker compose logs -f --tail=50
```

### Accés a l'aplicació

Un cop l'aplicació estigui en marxa:
- **API REST**: http://localhost:8080
- **Swagger/OpenAPI**: http://localhost:8080/swagger-ui.html
- **Admin**: http://localhost:8080/admin/login.html

---

## Desplegament al servidor (Manual)

Si voleu fare deploy manual sense esperar al CD automàtic:

1. **Generar el JAR:**
   ```powershell
   ./mvnw package -DskipTests
   ```

2. **Transferir el JAR al servidor:**
   ```bash
   scp target/backend-*.jar user@your-server:/home/user/backend/releases/
   ```

3. **Connectar al servidor i executar el JAR:**
   ```bash
   ssh user@your-server
   cd /home/user/backend
   
   export SAFESTEPS_DB_PASSWORD="your_password"
   nohup java -jar releases/backend-v1.0.jar --server.port=8081 --spring.config.location=file:/home/user/backend/application-local.properties > logs/logs_vX.X.log 2>&1 &
   ```

4. **Verificar logs:**
   Mirar la carpeta de logs, la versió que volem veure i el log corresponent:
   ```bash
   cat logs/logs_vX.X.log
   ```