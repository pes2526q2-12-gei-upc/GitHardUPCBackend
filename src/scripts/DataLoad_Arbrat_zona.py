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

log_filename = os.path.join(LOGS_DIR, f"{datetime.now().strftime('%Y-%m-%d_%H-%M-%S')}_dataload-arbrat-zona.log")

logging.basicConfig(
    level=logging.INFO,
    format='[%(levelname)s] %(asctime)s - %(message)s',
    datefmt='%H:%M:%S',
    handlers=[logging.FileHandler(log_filename), logging.StreamHandler()]
)

def load_properties(file_path, current_config):
    if not os.path.exists(file_path):
        return current_config
    try:
        with open(file_path, "r") as f:
            for line in f:
                line = line.strip()
                if "=" in line and not line.startswith("#"):
                    name, value = line.split("=", 1)
                    val = value.strip()
                    if val.startswith("${") and val.endswith("}"):
                        var_name = val[2:-1]
                        val = os.environ.get(var_name)
                    current_config[name.strip()] = val
        return current_config
    except Exception:
        return current_config

def get_config():
    config = {}
    config = load_properties(APP_PROPERTIES_PATH, config)
    config = load_properties(APP_LOCAL_PROPERTIES_PATH, config)
    return config

def get_latest_csv_url(api_url):
    logging.info("Consultant l'API d'Open Data per trobar l'enllaç del CSV...")
    try:
        headers = {'User-Agent': 'Mozilla/5.0'}
        response = requests.get(api_url, headers=headers)
        response.raise_for_status()
        data = response.json()
        if data.get("success"):
            resources = data["result"]["resources"]
            for res in resources:
                if res["format"].upper() == "CSV":
                    logging.info("URL Trobada: " + res["url"])
                    return res["url"]
        return None
    except Exception as e:
        logging.error(f"Error connectant amb l'API: {e}")
        return None

def main():
    logging.info("---------- Actualitzant dades de l'API d'Arbrat Zona ----------")
    config = get_config()

    url_api = config.get("opendata.arbrat_zona.url")
    t_arbrat_zona = config.get("db.table.arbrat_zona")
    db_user = config.get("spring.datasource.username")
    db_pass = config.get("spring.datasource.password")
    db_url_jdbc = config.get("spring.datasource.url")

    if not all([db_user, db_pass, db_url_jdbc, url_api, t_arbrat_zona]):
        logging.error("Falten paràmetres de configuració.")
        return

    try:
        clean_url = db_url_jdbc.replace("jdbc:", "")
        prefix, rest = clean_url.split("://")
        engine = create_engine(f"{prefix}://{db_user}:{db_pass}@{rest}")
    except Exception as e:
        logging.error(f"Error de connexió a la BD: {e}")
        return

    download_url = get_latest_csv_url(url_api)
    if not download_url:
        return

    try:
        logging.info("Iniciant descàrrega del contingut...")
        r = requests.get(download_url, headers={'User-Agent': 'Mozilla/5.0'})
        r.raise_for_status()

        # --- TRACTAMENT D'ENCODING ESPECIAL PER A BCN ---
        # Si detectem el caràcter nul \x00, és UTF-16
        if b'\x00' in r.content:
            logging.info("Format UTF-16 detectat. Decodificant...")
            contingut = r.content.decode('utf-16')
        else:
            contingut = r.text

        csv_data = StringIO(contingut)
        df = pd.read_csv(csv_data, sep=',', on_bad_lines='skip', engine='python')

        if df.shape[1] <= 1:
            logging.error("L'estructura continua tenint una sola columna.")
            return

        # Neteja estricta de noms de columnes per a PostgreSQL
        df.columns = [c.strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        # Actualització a la BD
        logging.info(f"Pujant {len(df)} files a la taula '{t_arbrat_zona}'...")
        with engine.begin() as conn:
            conn.execute(text(f'DROP TABLE IF EXISTS "{t_arbrat_zona}" CASCADE;'))

        df.to_sql(t_arbrat_zona, engine, if_exists='replace', index=False)
        logging.info(f"ÈXIT: Taula '{t_arbrat_zona}' creada amb {df.shape[1]} columnes.")

    except Exception as e:
        logging.error(f"Error en el processament: {e}")

    logging.info("---------- Dades de l'API d'Arbrat Zona actualitzades ----------")

if __name__ == "__main__":
    main()