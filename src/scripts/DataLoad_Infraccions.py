import os
import requests
import pandas as pd
import logging
from datetime import datetime
from sqlalchemy import create_engine, text
from io import StringIO

# Configuracio dels paths
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
APP_PROPERTIES_PATH = os.path.normpath(os.path.join(BASE_DIR, "..", "main", "resources", "application.properties"))
APP_LOCAL_PROPERTIES_PATH = os.path.normpath(os.path.join(BASE_DIR, "..", "main", "resources", "application-local.properties"))
LOGS_DIR = os.path.join(BASE_DIR, "logs")

if not os.path.exists(LOGS_DIR):
    os.makedirs(LOGS_DIR)

log_filename = os.path.join(LOGS_DIR, f"{datetime.now().strftime('%Y-%m-%d_%H-%M-%S')}_dataload-infraccions.log")

logging.basicConfig(
    level=logging.INFO,
    format='[%(levelname)s] %(asctime)s - %(message)s',
    datefmt='%H:%M:%S',
    handlers=[logging.FileHandler(log_filename), logging.StreamHandler()]
)

def load_properties(file_path, current_config):
    """Llegeix les propietats dels fitxers .properties"""
    if not os.path.exists(file_path):
        return current_config
    try:
        with open(file_path, "r") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#"):
                    key, value = line.split("=", 1)
                    current_config[key.strip()] = value.strip()
    except Exception as e:
        logging.warning(f"No s'ha pogut llegir {file_path}: {e}")
    return current_config

if __name__ == "__main__":
    logging.info("Iniciant el procés de càrrega de dades per Infraccions...")

    # Llegir configuració dels fitxers properties
    db_config = {}
    db_config = load_properties(APP_PROPERTIES_PATH, db_config)
    db_config = load_properties(APP_LOCAL_PROPERTIES_PATH, db_config)

    # Configuració de base de dades
    db_url_jdbc = db_config.get("spring.datasource.url")
    db_user = db_config.get("spring.datasource.username")
    db_pass = db_config.get("spring.datasource.password")

    # Configuració específica del dataset (llegida del properties)
    url = db_config.get("opendata.infraccions_joc.url")
    t_infraccions = db_config.get("db.table.infraccions_joc", "cat_infraccions_joc")

    if not all([db_url_jdbc, db_user, db_pass, url]):
        logging.error("Falten paràmetres de configuració (BD o URL) als fitxers properties.")
        exit(1)

    # Convertir URL JDBC a format SQLAlchemy
    try:
        clean_url = db_url_jdbc.replace("jdbc:", "")
        prefix, rest = clean_url.split("://")
        engine = create_engine(f"{prefix}://{db_user}:{db_pass}@{rest}")
    except Exception as e:
        logging.error(f"Error parsejant la URL de la BD: {e}")
        exit(1)

    try:
        logging.info(f"Descarregant el fitxer CSV des de: {url}")

        # Headers complets per simular un navegador i evitar l'error 403
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Accept': 'text/csv,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8',
            'Accept-Language': 'es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3',
            'Connection': 'keep-alive',
            'Upgrade-Insecure-Requests': '1'
        }

        r = requests.get(url, headers=headers, timeout=60)
        r.raise_for_status()

        # --- TRACTAMENT D'ENCODING ESPECIAL ---
        if b'\x00' in r.content:
            logging.info("Format UTF-16 detectat. Decodificant...")
            contingut = r.content.decode('utf-16')
        else:
            contingut = r.text

        # Llegim el CSV
        csv_data = StringIO(contingut)
        df = pd.read_csv(csv_data, sep=',', on_bad_lines='skip', engine='python')

        if df.shape[1] <= 1:
            logging.error("L'estructura de dades és incorrecta (només una columna). Revisa el separador.")
            exit(1)

        # Neteja de noms de columnes per a PostgreSQL
        df.columns = [
            c.strip().lower()
            .replace(' ', '_')
            .replace('.', '')
            .replace('(', '')
            .replace(')', '')
            .replace('ó', 'o')
            .replace('í', 'i')
            .replace('á', 'a')
            .replace('é', 'e')
            .replace('ú', 'u')
            .replace('à', 'a')
            .replace('è', 'e')
            .replace('ò', 'o')
            for c in df.columns
        ]

        # Actualització a la BD
        logging.info(f"Pujant {len(df)} files a la taula '{t_infraccions}'...")
        with engine.begin() as conn:
            # Eliminem la taula si existeix per reemplaçar-la
            conn.execute(text(f'DROP TABLE IF EXISTS "{t_infraccions}" CASCADE;'))

        # Bolcat de dades
        df.to_sql(t_infraccions, engine, if_exists='replace', index=False)

        logging.info("Procés completat amb èxit!")

    except requests.exceptions.HTTPError as errh:
        logging.error(f"Error HTTP: {errh}")
    except Exception as e:
        logging.error(f"S'ha produït un error durant el procés: {e}")