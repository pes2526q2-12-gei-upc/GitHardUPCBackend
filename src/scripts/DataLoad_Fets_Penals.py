import os
import requests
import pandas as pd
import logging
from datetime import datetime
from sqlalchemy import create_engine, text
from io import StringIO

# Configuració dels paths
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
APP_PROPERTIES_PATH = os.path.normpath(os.path.join(BASE_DIR, "..", "main", "resources", "application.properties"))
APP_LOCAL_PROPERTIES_PATH = os.path.normpath(os.path.join(BASE_DIR, "..", "main", "resources", "application-local.properties"))
LOGS_DIR = os.path.join(BASE_DIR, "logs")

if not os.path.exists(LOGS_DIR):
    os.makedirs(LOGS_DIR)

# Configuració segura del Logger (Evita el Security Hotspot S4792)
log_filename = os.path.join(LOGS_DIR, f"{datetime.now().strftime('%Y-%m-%d_%H-%M-%S')}_dataload-fets-penals.log")

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

# Handler per a fitxer i per a consola
file_handler = logging.FileHandler(log_filename)
stream_handler = logging.StreamHandler()
formatter = logging.Formatter('[%(levelname)s] %(asctime)s - %(message)s', datefmt='%H:%M:%S')

file_handler.setFormatter(formatter)
stream_handler.setFormatter(formatter)

if not logger.handlers:
    logger.addHandler(file_handler)
    logger.addHandler(stream_handler)

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

def main():
    logger.info("---------- Actualitzant dades de Fets Penals (Gencat) ----------")
    config = get_config()

    url_csv = config.get("opendata.fets_penals.url")
    t_fets = config.get("db.table.fets_penals")
    db_user = config.get("spring.datasource.username")
    db_pass = config.get("spring.datasource.password")
    db_url_jdbc = config.get("spring.datasource.url")

    # Comprovació de paràmetres
    params = {
        "url_csv": url_csv,
        "t_fets": t_fets,
        "db_user": db_user,
        "db_pass": db_pass,
        "db_url_jdbc": db_url_jdbc
    }

    missing = [k for k, v in params.items() if not v]
    if missing:
        logger.error("Falten paràmetres de configuració: %s", missing)
        return

    try:
        clean_url = db_url_jdbc.replace("jdbc:", "")
        prefix, rest = clean_url.split("://")
        engine = create_engine(f"{prefix}://{db_user}:{db_pass}@{rest}")
    except Exception as e:
        logger.error("Error de connexió a la BD: %s", e)
        return

    try:
        logger.info("Iniciant descàrrega del CSV...")
        r = requests.get(url_csv, headers={'User-Agent': 'Mozilla/5.0'})
        r.raise_for_status()

        # Llegim el CSV (Transparència Gencat sol anar en UTF-8)
        df = pd.read_csv(StringIO(r.text), sep=',', on_bad_lines='skip', engine='python')

        # Neteja de columnes per a SQL (minúscules i sense espais)
        df.columns = [str(c).strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        logger.info("Pujant %s files a la taula '%s'...", len(df), t_fets)
        with engine.begin() as conn:
            conn.execute(text(f'DROP TABLE IF EXISTS "{t_fets}" CASCADE;'))

        df.to_sql(t_fets, engine, if_exists='replace', index=False)
        logger.info("ÈXIT: Taula '%s' actualitzada correctament.", t_fets)

    except Exception as e:
        logger.error("Error en el processament: %s", e)

if __name__ == "__main__":
    main()