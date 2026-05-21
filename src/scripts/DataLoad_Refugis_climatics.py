import os
import sys
import requests
import pandas as pd
import logging
import zipfile
from datetime import datetime
from sqlalchemy import create_engine, text
from io import StringIO, BytesIO

# Configuracio dels paths
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
APP_PROPERTIES_PATH = os.path.normpath(os.path.join(BASE_DIR, "..", "main", "resources", "application.properties"))
APP_LOCAL_PROPERTIES_PATH = os.path.normpath(os.path.join(BASE_DIR, "..", "main", "resources", "application-local.properties"))
LOGS_DIR = os.path.join(BASE_DIR, "logs")

if not os.path.exists(LOGS_DIR):
    os.makedirs(LOGS_DIR)

log_filename = os.path.join(LOGS_DIR, f"{datetime.now().strftime('%Y-%m-%d_%H-%M-%S')}_dataload-refugis.log")

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
        with open(file_path, "r", encoding="utf-8") as f:
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
    local_path = sys.argv[1] if len(sys.argv) > 1 else APP_LOCAL_PROPERTIES_PATH
    config = load_properties(local_path, config)
    return config

def get_latest_resource_info(api_url):
    logging.info("Consultant l'API d'Open Data BCN per trobar l'enllaç del recurs de Refugis Climàtics...")
    try:
        headers = {'User-Agent': 'Mozilla/5.0'}
        response = requests.get(api_url, headers=headers)
        response.raise_for_status()
        data = response.json()
        if data.get("success"):
            resources = data["result"]["resources"]

            # Primer busquem si hi ha algun CSV directe
            for res in resources:
                if res["format"].upper() == "CSV":
                    logging.info("URL Trobada (CSV): " + res["url"])
                    return res["url"], "CSV"

            # Si no n'hi ha, busquem en ZIP
            for res in resources:
                if res["format"].upper() == "ZIP":
                    logging.info("URL Trobada (ZIP): " + res["url"])
                    return res["url"], "ZIP"

        return None, None
    except Exception as e:
        logging.error(f"Error connectant amb l'API: {e}")
        return None, None

def main():
    logging.info("---------- Actualitzant dades de l'API de Refugis Climàtics (BCN) ----------")
    config = get_config()

    # Agafem les variables específiques dels refugis
    url_api = config.get("opendata.refugis.url")
    t_refugis = config.get("db.table.refugis")

    db_user = config.get("spring.datasource.username")
    db_pass = config.get("spring.datasource.password")
    db_url_jdbc = config.get("spring.datasource.url")

    params = {
        "url_api": url_api,
        "t_refugis": t_refugis,
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

    download_url, file_format = get_latest_resource_info(url_api)
    if not download_url:
        logging.error("No s'ha trobat cap recurs CSV ni ZIP per a aquest dataset.")
        return

    try:
        logging.info("Iniciant descàrrega del contingut...")
        r = requests.get(download_url, headers={'User-Agent': 'Mozilla/5.0'})
        r.raise_for_status()

        # Procés d'extracció segons el format
        if file_format == "ZIP":
            logging.info("Format ZIP detectat. Extreient les dades en memòria...")
            with zipfile.ZipFile(BytesIO(r.content)) as z:
                nom_fitxer = z.namelist()[0]
                contingut_cru = z.read(nom_fitxer)
        else:
            contingut_cru = r.content

        # Tractament d'encoding per si de cas
        if b'\x00' in contingut_cru:
            logging.info("Format UTF-16 detectat. Decodificant...")
            contingut = contingut_cru.decode('utf-16')
        else:
            contingut = contingut_cru.decode('utf-8', errors='replace')

        csv_data = StringIO(contingut)

        logging.info("Llegint les dades i preparant columnes...")
        df = pd.read_csv(csv_data, sep=',', on_bad_lines='skip', engine='python')

        # Neteja de columnes per compatibilitat amb PostgreSQL
        df.columns = [str(c).strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        logging.info(f"Pujant {len(df)} files a la taula '{t_refugis}'...")
        with engine.begin() as conn:
            conn.execute(text(f'TRUNCATE TABLE "{t_refugis}" RESTART IDENTITY CASCADE;'))

        df.to_sql(t_refugis, engine, if_exists='append', index=False)
        logging.info(f"ÈXIT: Taula '{t_refugis}' actualitzada correctament.")

    except Exception as e:
        logging.error(f"Error en el processament: {e}")

if __name__ == "__main__":
    main()