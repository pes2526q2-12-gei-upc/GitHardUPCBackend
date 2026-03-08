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
# Fitxer amb noms de taules i vista, i URL de l'API
APP_PROPERTIES_PATH = os.path.normpath(os.path.join(BASE_DIR, "..", "main", "resources", "application.properties"))
# Fitxer amb dades locals
APP_LOCAL_PROPERTIES_PATH = os.path.normpath(os.path.join(BASE_DIR, "..", "main", "resources", "application-local.properties"))
LOGS_DIR = os.path.join(BASE_DIR, "logs")


# Cal crear el directori de logs si no existeix
if not os.path.exists(LOGS_DIR):
    os.makedirs(LOGS_DIR)


# Configuracio dels logs: un fitxer per dia i tambe mostrar per consola

# El nom del fitxer sera any-mes-dia_dataload-grafviari.log, per exemple: 2026-04-23_dataload-grafviari.log
log_filename = os.path.join(LOGS_DIR, f"{datetime.now().strftime('%Y-%m-%d_%H-%M-%S')}_dataload-grafviari.log")
# Configuracio del logging: INFO per consola i fitxer
logging.basicConfig(
    # Volem que el log mostri aquells missatges d'INFO o superiors (INFO, WARNING, ERROR, CRITICAL)
    level=logging.INFO,
    # El format dels missatges de log inclou la data, el nivell i el missatge
    # Exemple: [ERROR] 2026-04-23 15:30:45 - msg
    format='[%(levelname)s] %(asctime)s - %(message)s',
    datefmt='%H:%M:%S',
    # Volem enviar els missatges tant a un fitxer com a la consola
    handlers=[
        logging.FileHandler(log_filename),
        logging.StreamHandler()
    ]
)

try:
    # Nomes permetre que el propietari del fitxer de log pugui llegir i escriure, per a protegir la confidencialitat de les dades de log.
    os.chmod(log_filename, stat.S_IRUSR | stat.S_IWUSR)
except Exception:
    logging.warning("No s'han pogut aplicar els permisos estrictes al fitxer de log (possiblement estàs a Windows).")

# Funcio per a carregar la configuracio d'un fitxer de propietats. Permet resoldre variables d'entorn tipus ${VAR}
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
                    # Resolucio de EVs en format ${VAR}
                    if val.startswith("${") and val.endswith("}"):
                        var_name = val[2:-1]
                        val = os.environ.get(var_name)
                        if val is None:
                            logging.warning(f"Variable d'entorn '{var_name}' no definida al sistema.")
                    current_config[name.strip()] = val
        return current_config
    except Exception:
        logging.error(f"Error llegint {file_path}.")
        return current_config


# Funcio per a carregar la configuració general.
def get_config():
    config = {}
    # Carrega de la configuracio de taules, vistes i URL de l'API.
    config = load_properties(APP_PROPERTIES_PATH, config)
    # Carrega de la configuracio local.
    config = load_properties(APP_LOCAL_PROPERTIES_PATH, config)
    return config


# Funcio per refrescar la vista SQL que defineix el graf de rutes.
# Vista necessaria per a que el motor de rutes funcioni correctament per tant, es refresca cada cop que es carreguen les dades.
# A la vista no ens interessa carregar trams que siguin viaducte o nus, ja que no formen part de les rutes transitables.
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
    except Exception:
        logging.error(f"Error creant la vista {v_name}.")


# Necessitem consultar l'API d'Open Data BCN per a trobar la URL del ZIP mes recent, ja que aquesta URL pot canviar amb cada actualitzacio de dades.
def get_latest_zip_url(api_url):
    logging.info("Consultant l'API d'Open Data per trobar la versio mes recent...")
    try:
        # Preguntem a l'api d'Open Data BCN per a obtenir la llista de recursos disponibles del dataset de graf vial, i busquem el que sigui un ZIP.
        response = requests.get(api_url)
        response.raise_for_status()
        data = response.json()

        # Agafem aquella url del recurs que sigui un ZIP.
        if data.get("success"):
            resources = data["result"]["resources"]
            for res in resources:
                if res["format"].upper() == "ZIP" or "zip" in res["url"].lower():
                    logging.info("URL Trobada per al ZIP: " + res["url"])
                    return res["url"]

        logging.error("No s'ha trobat cap recurs en format ZIP al dataset.")
        return None
    except Exception:
        logging.error("Error connectant amb l'API d'Open Data BCN.")
        return None



def main():
    logging.info("---------- Actualitzant dades de l'API GrafVial ----------")

    config = get_config()

    # Valors d'application.properties
    url_zip = config.get("opendata.grafvial.url")
    t_trams = config.get("db.table.trams")
    t_nodes = config.get("db.table.nodes")
    v_rutes = config.get("db.view.rutes")

    # Valors d'application-local.properties
    db_user = config.get("spring.datasource.username")
    db_pass = config.get("spring.datasource.password")
    db_url_jdbc = config.get("spring.datasource.url")

    if not all([db_user, db_pass, db_url_jdbc, url_zip, t_trams, t_nodes, v_rutes]):
        logging.error("Algun parametre de configuracio no s'ha carregat correctament o no esta definit. Revisa els fitxers de propietats i les variables d'entorn.")
        return

    # Connexio BD
    try:
        clean_url = db_url_jdbc.replace("jdbc:", "")
        prefix, rest = clean_url.split("://")
        engine = create_engine(f"{prefix}://{db_user}:{db_pass}@{rest}")
    except Exception:
        logging.error(f"Error de connexio a la DB. Revisa la configuracio de la DB!")
        return

    # Obtenir la URL del ZIP mes recent a traves de l'API d'Open Data BCN
    download_url = get_latest_zip_url(url_zip)
    if not download_url:
        logging.error("No s'ha pogut obtenir l'URL del ZIP de l'API. No es poden actualitzar les dades.")
        return

    # Descarrega de dades
    try:
        logging.info("Descarregant ZIP d'Open Data...")
        logging.info(f"URL: {download_url}")
        response = requests.get(download_url)
        response.raise_for_status()
        zip_file = zipfile.ZipFile(io.BytesIO(response.content))

        for filename in zip_file.namelist():
            if filename.lower().endswith('.csv'):
                if "trams" in filename.lower(): table_name = t_trams
                elif "nodes" in filename.lower(): table_name = t_nodes
                else: continue

                # Necessitem esborrar les taules abans de carregar les noves dades.
                # Utilitzem cascade per les vistes que apuntin a aquestes taules.
                with engine.begin() as conn:
                    conn.execute(text(f'DROP TABLE IF EXISTS "{table_name}" CASCADE;'))

                logging.info(f"Processant fitxer amb taula de destinacio '{table_name}'")
                df = pd.read_csv(zip_file.open(filename), sep=';', encoding='latin1', low_memory=False)
                df.to_sql(table_name, engine, if_exists='replace', index=False)
                logging.info(f"Taula '{table_name}' actualitzada.")

        refresh_routing_view(engine, v_rutes, t_trams, t_nodes)

    except Exception:
        logging.error(f"Error durant el proces. Revisa la URL de l'API i la connexio a la DB.")
        return

    logging.info("---------- Dades de l'API GrafVial actualitzades ----------")

if __name__ == "__main__":
    main()