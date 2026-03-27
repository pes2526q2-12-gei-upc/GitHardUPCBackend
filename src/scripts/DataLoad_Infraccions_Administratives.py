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

log_filename = os.path.join(LOGS_DIR, f"{datetime.now().strftime('%Y-%m-%d_%H-%M-%S')}_dataload-infraccions-joc.log")

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

def main():
    logging.info("---------- Actualitzant dades de Infraccions de Joc (Gencat) ----------")
    config = get_config()

    url_csv = config.get("opendata.infraccions_joc.url")
    t_joc = config.get("db.table.infraccions_joc")
    db_user = config.get("spring.datasource.username")
    db_pass = config.get("spring.datasource.password")
    db_url_jdbc = config.get("spring.datasource.url")

    # Comprovació de paràmetres (per evitar l'error de configuració buida)
    params = {
        "url_csv": url_csv,
        "t_joc": t_joc,
        "db_user": db_user,
        "db_pass": db_pass,
        "db_url_jdbc": db_url_jdbc
    }
    missing = [k for k, v in params.items() if not v]
    if missing:
        logging.error(f"Falten paràmetres de configuració: {missing}")
        return

    try:
        clean_url = db_url_jdbc.replace("jdbc:", "")
        prefix, rest = clean_url.split("://")
        engine = create_engine(f"{prefix}://{db_user}:{db_pass}@{rest}")
    except Exception as e:
        logging.error(f"Error de connexió a la BD: {e}")
        return

    try:
        logging.info("Iniciant descàrrega del CSV de Infraccions...")
        r = requests.get(url_csv, headers={'User-Agent': 'Mozilla/5.0'})
        r.raise_for_status()

        # Llegim el CSV
        df = pd.read_csv(StringIO(r.text), sep=',', on_bad_lines='skip', engine='python')

        # Neteja de columnes per a PostgreSQL (minúscules i sense espais)
        df.columns = [str(c).strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        logging.info(f"Pujant {len(df)} files a la taula '{t_joc}'...")
        with engine.begin() as conn:
            conn.execute(text(f'DROP TABLE IF EXISTS "{t_joc}" CASCADE;'))

        df.to_sql(t_joc, engine, if_exists='replace', index=False)
        logging.info(f"ÈXIT: Taula '{t_joc}' actualitzada correctament.")

    except Exception as e:
        logging.error(f"Error en el processament: {e}")

if __name__ == "__main__":
    main()