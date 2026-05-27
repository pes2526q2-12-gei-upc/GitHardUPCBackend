# GitHardUPC Backend

Aquest és el repositori del backend del projecte **GitHardUPC**. A continuació trobareu les instruccions detallades per a la configuració i instal·lació del projecte en el vostre entorn local, així com el funcionament de la nostra infraestructura automatitzada al servidor.

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

## Configuració del Projecte (Entorn Local)

Seguiu aquests passos per configurar l'entorn de desenvolupament local:

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
2. Enganxeu-lo a la maixa carpeta i anomeneu-lo `application-local.properties`.
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

## Configuració de Scripts de Python al Servidor

Per al correcte funcionament de les tasques concurrents del Backend (com els Schedulers o os scripts de càrrega de dades), s'ha de mantenir la mateixa estructura de fitxers que en local:

1. **Pujar els scripts manualment:** Els scripts de Python s'hande pujar al servidor de manera manual i col·locar-se exactament en el mateix directori base on resideixen els executables del backend, respectant la ruta: `src/scripts/`.
2. **Per instal·lar les dependències de Python:** Es recomana crear un entorn virtual (`.venv`) dins de la carpeta `src/scripts/` i instal·lar-hi les dependències amb `pip install -r requirements.txt`.
3. **Logs d'execució dels scripts:** Tots els fitxers de logs relacionats amb l'execució i el processament d'aquests scripts es generaran de manera automàtica dins d'aquesta mateixa carpeta `src/scripts/`.

---

## Configuració de Continuous Integration / Deployment (CI/CD)

El projecte està configurat amb **GitHub Actions** i un **Runner local (Self-Hosted)** allotjat al propi servidor per a CI/CD automàtic. Els fitxers de configuració es troben a `.github/workflows/`:

### Workflows Configurats

#### 1. **CI (Integració Contínua)** - `backend-ci.yml`
S'executa automàticament en cada **push** o **pull request** a les branques: `main`, `develop`, `config/Integracio_Continua`

**Passos:**
- Checkout del codi
- Instal·lació de JDK 21
- Compilació i execució de tests (`./mvnw clean verify`)
- Anàlisi de codi amb SonarQube

#### 2. **CD (Continuous Deployment)** - `backend-cd.yml`
S'executa **automàticament** quan el CI passa correctament (build verd) a les branques: `main` o `develop`.
Aquest workflow compta amb un filtre de seguretat estricte per ignorar execucions de forks externs.

**Passos:**
- Compilació de l'aplicació (`./mvnw package`)
- Generació del JAR directament al workspace local del Runner
- Desplegament automàtic mitjançant un sistema d'historial a la carpeta `/releases/` i enllaços simbòlics (*symlinks*).
- Reinici automàtic del servei assignat mitjançant `systemd`:
    - **main**: Versió major (v1.0, v2.0, etc.) al port **8081** (producció) -> Servei `safesteps-main`
    - **develop**: Versió minor (v1.1, v1.2, etc.) al port **8082** (staging) -> Servei `safesteps-develop`

### Configuració de Secrets de GitHub

Per fer funcionar el CD s'han de configurar els secrets següents a l'apartat **Settings** > **Secrets and Variables** > **Actions**:

| Secret | Descripció | Exemple |
|--------|----------------------------------------------------|--------------------------------|
| `SERVER_USER` | Usuari local del servidor VirTech | `alumne` |
| `SAFESTEPS_DB_PASSWORD` | Contrasenya de la base de dades PostgreSQL per al CI | `your_secure_password` |
| `SONAR_TOKEN` | Token de SonarQube per a l'anàlisi de codi | Token generat a SonarQube |

> [!WARNING]
> **No pugeu secrets reals al vostre repositori!** Els secrets de GitHub s'emmagatzemen de forma segura.

---

## Configuració Inicial de la Infraestructura al Servidor (Sysadmin)

Si s'ha de configurar el servidor des de zero o realitzar un desplegament completament manual de la infraestructura de serveis, seguiu aquests passos directament connectats per SSH a VirTech:

### 1. Preparació de Fitxers Locals del Servidor
A la carpeta d'execució principal `/home/alumne/backend/` hi han de residir els fitxers de propietats que estan exclosos de Git:
* `application-local.properties` (Configurat per al port `8082` de Staging)
* `application-local_prod.properties` (Configurat per al port `8081` de Producció)

### 2. Creació dels Serveis de Linux (systemd)
S'han de crear dos fitxers de configuració de serveis a la ruta del sistema operatiu per gestionar les instàncies de forma aïllada i automatitzada.

#### Instància de Develop/Staging (`/etc/systemd/system/safesteps-develop.service`):
```ini
[Unit]
Description=SafeSteps Backend Staging (Develop)
After=network.target

[Service]
User=alumne
WorkingDirectory=/home/alumne/backend
ExecStart=/usr/bin/env SAFESTEPS_DB_PASSWORD='la_vostra_contrasenya' /usr/bin/java -Xmx768m -XX:+UseSerialGC -jar /home/alumne/backend/deployedVersion-develop.jar --spring.config.location=file:/home/alumne/backend/application-local.properties
Restart=always
RestartSec=30
StandardOutput=syslog
StandardError=syslog
SyslogIdentifier=safesteps-develop

[Install]
WantedBy=multi-user.target
```

#### Instància de Main/Producció (`/etc/systemd/system/safesteps-main.service`):
```ini
[Unit]
Description=SafeSteps Backend Production (Main)
After=network.target

[Service]
User=alumne
WorkingDirectory=/home/alumne/backend
ExecStart=/usr/bin/env SAFESTEPS_DB_PASSWORD='la_vostra_contrasenya' /usr/bin/java -Xmx768m -XX:+UseSerialGC -jar /home/alumne/backend/deployedVersion-main.jar --spring.config.location=file:/home/alumne/backend/application-local_prod.properties
Restart=always
RestartSec=30
StandardOutput=syslog
StandardError=syslog
SyslogIdentifier=safesteps-main

[Install]
WantedBy=multi-user.target
```

### 3. Configuració de Permisos per al Runner (`visudo`)
Per permetre que la pipeline de GitHub Actions apliqui els canvis de codi reiniciant els serveis de forma calenta sense demanar contrasenyes, s'ha de configurar un fitxer de drets d'usuari de sistema:

1. Executar: `sudo visudo -f /etc/sudoers.d/safesteps`
2. Introduir la següent configuració en una línia:
```text
alumne ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart safesteps-develop, /usr/bin/systemctl status safesteps-develop, /usr/bin/systemctl restart safesteps-main, /usr/bin/systemctl status safesteps-main
```

### 4. Activació dels Serveis
Per registrar els nous fitxers de configuració al nucli de Linux i activar-ne la persistència (arrencada automàtica si el servidor es reinicia):
```bash
sudo systemctl daemon-reload
sudo systemctl enable safesteps-develop
sudo systemctl enable safesteps-main
```

---

## Gestió de Serveis i Logs al Servidor (Manteniment)

Una vegada configurada la infraestructura, podeu utilitzar les següents comandes per al manteniment manual i monitorització de la salut de l'aplicació en segon pla:

### Control dels Serveis en Calent
```bash
# Aturar l'entorn de Staging o Producció
sudo systemctl stop safesteps-develop
sudo systemctl stop safesteps-main

# Engegar o reiniciar els entorns de forma manual
sudo systemctl start safesteps-develop
sudo systemctl restart safesteps-develop

# Comprovar l'estat d'execució per veure si estan actius (active running)
systemctl status safesteps-develop
systemctl status safesteps-main
```

### Visualització de Logs
Els logs de l'aplicació s'emmagatzemen directament al gestor de diaris centralitzat de Linux, anomenat `journalctl`:

```bash
# Veure els logs de PRODUCCIÓ (main) en temps real (stream)
journalctl -u safesteps-main -f

# Veure os logs de STAGING (develop) en temps real (stream)
journalctl -u safesteps-develop -f

# Veure les últimes 100 línies de l'entorn de develop
journalctl -u safesteps-develop -n 100

# Filtrar només els errors d'execució i excepcions de Java
journalctl -u safesteps-develop -p err
```
*(Per sortir de la pantalla de logs de `journalctl`, premeu la tecla **`q`**).*

---

## Docker

Si voleu aixecar els serveis de suport en local (com la base de dades PostgreSQL) utilitzant Docker:

1. Baixeu i instal·leu Docker Desktop.
2. Reinicieu el PC i executeu Docker Desktop.
3. Comproveu que està en "Engine running".
4. Executeu la següent comanda a l'arrel del projecte:

```bash
docker compose up -d
```

---

## Execució de l'Aplicació (Local)

Un cop configurat tot, podeu executar l'aplicació amb Maven (en local) o amb els scripts preparats.

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

Un cop l'aplicació estigui en marxa en local:
- **API REST**: http://localhost:8080
- **Swagger/OpenAPI**: http://localhost:8080/swagger-ui.html
- **Admin**: http://localhost:8080/admin/login.html