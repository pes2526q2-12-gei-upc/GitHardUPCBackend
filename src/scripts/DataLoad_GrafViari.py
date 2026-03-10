import os
import requests
import zipfile
import io
import pandas as pd
import logging
import stat
from datetime import datetime
from sqlalchemy import create_engine, text

# Configuracio dels paths
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
APP_PROPERTIES_PATH = os.path.normpath(os.path.join(BASE_DIR, "..", "main", "resources", "application.properties"))
APP_LOCAL_PROPERTIES_PATH = os.path.normpath(os.path.join(BASE_DIR, "..", "main", "resources", "application-local.properties"))
LOGS_DIR = os.path.join(BASE_DIR, "logs")

# Cal crear el directori de logs si no existeix
if not os.path.exists(LOGS_DIR):
    os.makedirs(LOGS_DIR)

# Configuracio dels logs
log_filename = os.path.join(LOGS_DIR, f"{datetime.now().strftime('%Y-%m-%d_%H-%M-%S')}_dataload-grafviari.log")
logging.basicConfig(
    level=logging.INFO,
    format='[%(levelname)s] %(asctime)s - %(message)s',
    datefmt='%H:%M:%S',
    handlers=[
        logging.FileHandler(log_filename),
        logging.StreamHandler()
    ]
)

try:
    os.chmod(log_filename, stat.S_IRUSR | stat.S_IWUSR)
except Exception:
    logging.warning("No s'han pogut aplicar els permisos estrictes al fitxer de log (possiblement estàs a Windows).")

# Funcio per a carregar la configuracio d'un fitxer de propietats
def load_properties(file_path, current_config):
    if not os.path.exists(file_path):
        logging.warning(f"No s'ha trobat el fitxer: {file_path}")
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
                        if val is None:
                            logging.warning(f"Variable d'entorn '{var_name}' no definida al sistema.")
                    current_config[name.strip()] = val
        return current_config
    except Exception as e:
        logging.error(f"Error llegint {file_path}: {e}")
        return current_config

# Funcio per a carregar la configuració general
def get_config():
    config = {}
    config = load_properties(APP_PROPERTIES_PATH, config)
    config = load_properties(APP_LOCAL_PROPERTIES_PATH, config)
    return config

# Funcio per refrescar la vista SQL
def refresh_routing_view(engine, v_name, t_trams, t_nodes):
    view_sql = f"""
    CREATE OR REPLACE VIEW {v_name} AS
    SELECT 
        t."FID" AS fid, 
        n_inici."FID" AS source, 
        n_final."FID" AS target,
        t."LONGITUD" AS longitud, 
        t."NVia_D" AS nom_carrer
    FROM {t_trams} t
    JOIN {t_nodes} n_inici ON t."C_Nus_I" = n_inici."C_Nus"
    JOIN {t_nodes} n_final ON t."C_Nus_F" = n_final."C_Nus"
    WHERE t."TVia_D" NOT IN ('Viaducte', 'Nus', '-', ' ', '');
    """
    try:
        with engine.begin() as conn:
            conn.execute(text(view_sql))
            logging.info(f"Vista '{v_name}' actualitzada correctament.")
    except Exception as e:
        logging.error(f"Error creant la vista {v_name}: {e}")

# Funcio per obtenir la URL del ZIP
def get_latest_zip_url(api_url):
    logging.info("Consultant l'API d'Open Data per trobar la versio mes recent...")
    try:
        response = requests.get(api_url)
        response.raise_for_status()
        data = response.json()

        if data.get("success"):
            resources = data["result"]["resources"]
            for res in resources:
                if res["format"].upper() == "ZIP" or "zip" in res["url"].lower():
                    logging.info("URL Trobada per al ZIP: " + res["url"])
                    return res["url"]

        logging.error("No s'ha trobat cap recurs en format ZIP al dataset.")
        return None
    except Exception as e:
        logging.error(f"Error connectant amb l'API d'Open Data BCN: {e}")
        return None

def _process_and_swap_table(engine, zip_file, filename, target_table_name):
    """
    Llegeix un CSV des del ZIP, valida la seva integritat, el carrega en una taula de staging,
    i finalment realitza un swap atòmic cap a la taula de producció.
    """
    staging_table = f"{target_table_name}_staging"

    logging.info(f"Processant el fitxer '{filename}' cap a la taula '{target_table_name}'...")
    df = pd.read_csv(zip_file.open(filename), sep=';', encoding='latin1', low_memory=False)

    # 1. Health Check (Validación de qualitat)
    MIN_EXPECTED_ROWS = 1000
    if df.empty or len(df) < MIN_EXPECTED_ROWS:
        error_msg = f"Health check fallit per a {filename}. Registres: {len(df)}. S'avorta la càrrega."
        logging.error(error_msg)
        raise ValueError(error_msg)

    logging.info(f"Carregant {len(df)} registres a la taula temporal '{staging_table}'...")
    # 2. Càrrega a Staging
    df.to_sql(staging_table, engine, if_exists='replace', index=False)

    # 3. Transacció Atòmica (Swapping)
    logging.info(f"Iniciant Swap Atòmic: '{staging_table}' -> '{target_table_name}'...")
    try:
        with engine.begin() as conn:
            conn.execute(text(f'DROP TABLE IF EXISTS "{target_table_name}" CASCADE;'))
            conn.execute(text(f'ALTER TABLE "{staging_table}" RENAME TO "{target_table_name}";'))
        logging.info(f"¡Èxit! La taula '{target_table_name}' està ara en producció.")
    except Exception as e:
        logging.critical(f"Error crític en el swap de la taula '{target_table_name}': {e}")
        raise

def main():
    logging.info("---------- Actualitzant dades de l'API GrafVial ----------")

    config = get_config()

    url_zip = config.get("opendata.grafvial.url")
    t_trams = config.get("db.table.trams")
    t_nodes = config.get("db.table.nodes")
    v_rutes = config.get("db.view.rutes")

    db_user = config.get("spring.datasource.username")
    db_pass = config.get("spring.datasource.password")
    db_url_jdbc = config.get("spring.datasource.url")

    if not all([db_user, db_pass, db_url_jdbc, url_zip, t_trams, t_nodes, v_rutes]):
        logging.error("Algun parametre de configuracio no s'ha carregat correctament o no esta definit.")
        return

    # Connexio BD
    try:
        clean_url = db_url_jdbc.replace("jdbc:", "")
        prefix, rest = clean_url.split("://")
        engine = create_engine(f"{prefix}://{db_user}:{db_pass}@{rest}")
    except Exception as e:
        logging.error(f"Error de connexio a la DB. Revisa la configuracio de la DB! Detall: {e}")
        return

    # Obtenir URL
    download_url = get_latest_zip_url(url_zip)
    if not download_url:
        logging.error("No s'ha pogut obtenir l'URL del ZIP. No es poden actualitzar les dades.")
        return

    # Descarrega i proces de dades
    try:
        logging.info("Descarregant ZIP d'Open Data...")
        response = requests.get(download_url)
        response.raise_for_status()
        zip_file = zipfile.ZipFile(io.BytesIO(response.content))

        for filename in zip_file.namelist():
            if filename.lower().endswith('.csv'):
                if "trams" in filename.lower():
                    _process_and_swap_table(engine, zip_file, filename, t_trams)
                elif "nodes" in filename.lower():
                    _process_and_swap_table(engine, zip_file, filename, t_nodes)

        # Refrescar vista si tot ha anat be
        refresh_routing_view(engine, v_rutes, t_trams, t_nodes)

    except Exception as e:
        logging.error(f"Error durant el proces d'actualitzacio: {e}")
        return

    logging.info("---------- Dades de l'API GrafVial actualitzades ----------")

# Entry point correcte
if __name__ == "__main__":
    main()