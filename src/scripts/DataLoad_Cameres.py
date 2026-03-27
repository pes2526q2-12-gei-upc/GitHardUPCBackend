import os
import requests
import pandas as pd
import logging
import zipfile
from datetime import datetime
from sqlalchemy import create_engine, text
from io import StringIO, BytesIO

# Configuració dels paths
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
APP_PROPERTIES_PATH = os.path.normpath(os.path.join(BASE_DIR, "..", "main", "resources", "application.properties"))
APP_LOCAL_PROPERTIES_PATH = os.path.normpath(os.path.join(BASE_DIR, "..", "main", "resources", "application-local.properties"))
LOGS_DIR = os.path.join(BASE_DIR, "logs")

if not os.path.exists(LOGS_DIR):
    os.makedirs(LOGS_DIR)

# Millora de seguretat per al Logger (S4792)
log_filename = os.path.join(LOGS_DIR, f"{datetime.now().strftime('%Y-%m-%d_%H-%M-%S')}_dataload-cameres.log")

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

# Handler per a fitxer i consola
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

def get_latest_resource_info(api_url):
    logger.info("Consultant l'API d'Open Data per trobar l'enllaç de les dades...")
    try:
        headers = {'User-Agent': 'Mozilla/5.0'}
        response = requests.get(api_url, headers=headers)
        response.raise_for_status()
        data = response.json()

        if data.get("success"):
            resources = data["result"]["resources"]
            formats_permesos = ["CSV", "JSON", "ZIP"]
            for fmt in formats_permesos:
                for res in resources:
                    if res["format"].upper() == fmt:
                        logger.info("Recurs trobat. Format: %s | URL: %s", fmt, res["url"])
                        return res["url"], fmt
        return None, None
    except Exception as e:
        logger.error("Error connectant amb l'API: %s", e)
        return None, None

def read_data_into_dataframe(file_format, content):
    """Llegeix el contingut descarregat sense demanar la URL que no es fa servir."""
    try:
        if file_format == 'ZIP':
            logger.info("Format ZIP detectat. Extreient dades...")
            with zipfile.ZipFile(BytesIO(content)) as z:
                for filename in z.namelist():
                    if filename.lower().endswith('.csv'):
                        logger.info("Llegint %s des del ZIP...", filename)
                        with z.open(filename) as f:
                            return pd.read_csv(f, sep=',', on_bad_lines='skip', engine='python')
                    elif filename.lower().endswith('.json'):
                        logger.info("Llegint %s des del ZIP...", filename)
                        with z.open(filename) as f:
                            return pd.read_json(f)
            return None

        elif file_format == 'JSON':
            return pd.read_json(StringIO(content.decode('utf-8')))

        else: # CSV
            contingut = content.decode('utf-16') if b'\x00' in content else content.decode('utf-8')
            return pd.read_csv(StringIO(contingut), sep=',', on_bad_lines='skip', engine='python')

    except Exception as e:
        logger.error("Error processant dades %s: %s", file_format, e)
        return None

def main():
    logger.info("---------- Iniciant Actualització de Càmeres ----------")
    config = get_config()

    url_api = config.get("opendata.cameres.url")
    t_cameres = config.get("db.table.cameres")
    db_user = config.get("spring.datasource.username")
    db_pass = config.get("spring.datasource.password")
    db_url_jdbc = config.get("spring.datasource.url")

    if not all([db_user, db_pass, db_url_jdbc, url_api, t_cameres]):
        logger.error("Falten paràmetres de configuració.")
        return

    try:
        clean_url = db_url_jdbc.replace("jdbc:", "")
        prefix, rest = clean_url.split("://")
        engine = create_engine(f"{prefix}://{db_user}:{db_pass}@{rest}")

        download_url, file_format = get_latest_resource_info(url_api)
        if not download_url:
            return

        r = requests.get(download_url, headers={'User-Agent': 'Mozilla/5.0'})
        r.raise_for_status()

        df = read_data_into_dataframe(file_format, r.content)

        if df is not None and not df.empty:
            df.columns = [str(c).strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

            with engine.begin() as conn:
                conn.execute(text(f'DROP TABLE IF EXISTS "{t_cameres}" CASCADE;'))

            df.to_sql(t_cameres, engine, if_exists='replace', index=False)
            logger.info("ÈXIT: Taula '%s' creada amb %s files.", t_cameres, len(df))

    except Exception as e:
        logger.error("Error en el procés: %s", e)

if __name__ == "__main__":
    main()