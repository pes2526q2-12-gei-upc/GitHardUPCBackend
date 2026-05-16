# GitHardUPC Backend

Aquest és el repositori del backend del projecte **GitHardUPC**. A continuació trobareu les instruccions detallades per a la configuració i instal·lació del projecte en el vostre entorn local.

## Members:

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
* **Java 17** o superior
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

### 2. Propietats Locals (`application-local.properties`)
Heu de crear un fitxer de configuració local per sobreescriure les propietats per defecte:
1. Copieu el fitxer `src/main/resources/application-local.properties.example`.
2. Enganxeu-lo a la mateixa carpeta i anomeneu-lo `application-local.properties`.
3. Configureu les següents dades segons el vostre entorn:

#### Base de Dades (BD)
Actualitzeu la URL, l'usuari i la contrasenya de la vostra base de dades:

spring.datasource.url=jdbc:postgresql://vostra_url:port/nom_bd
spring.datasource.username=el_vostre_usuari


#### Contrassenya d'Administrador i Tokens
Configureu la contrasenya del panell d'administració i el token de l'API externa:

admin.password=la_vostra_contrasenya_admin
api.external.token=el_vostre_token_extern


### 3. Variables d'Entorn
Per seguretat, la contrasenya de la base de dades s'ha de configurar com una variable d'entorn anomenada `SAFESTEPS_DB_PASSWORD`.

* **Windows (PowerShell):**

  setx SAFESTEPS_DB_PASSWORD "la_vostra_contrasenya"

* **IntelliJ IDEA:**
  `Run` -> `Edit Configurations` -> `Environment Variables` -> Afegir `SAFESTEPS_DB_PASSWORD=la_vostra_contrasenya`.

### 4. Configuració de Scripts i Python
Assegureu-vos que les rutes dels scripts i de l'executable de Python siguin correctes en el vostre `application-local.properties`:

backend.scheduler.python.command=python
backend.scheduler.scripts.path=src/scripts/

> [!IMPORTANT]
> Comproveu que el comando `python` es el correcte en el vostre sistema, per exemple en Windows es pot utilitzar `python3` o la ruta directa a l'executable de python del sistema o d'un entorn virtual.

---

## Docker

Si voleu aixecar els serveis (com la base de dades) utilitzant Docker:

1. Baixeu i instal·leu Docker Desktop.
2. Reinicieu el pc i executeu Docker Desktop.
3. Comproveu que esta en "Engine running".
4. Executeu la següent comanda a l'arrel del projecte:

   docker-compose up -d

---

## Execució de l'Aplicació

Un cop configurat tot, podeu executar l'aplicació amb Maven:

./mvnw run


O utilitzant el vostre IDE (IntelliJ, Eclipse, etc.) executant la classe principal `BackendApplication`.
