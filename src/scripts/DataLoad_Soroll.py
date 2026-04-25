import os
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

log_filename = os.path.join(LOGS_DIR, f"{datetime.now().strftime('%Y-%m-%d_%H-%M-%S')}_dataload-soroll.log")

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

def get_latest_resource_info(api_url):
    """Funció del teu codi original, perfecta per navegar per l'API de CKAN"""
    logging.info(f"Consultant l'API d'Open Data BCN per: {api_url}")
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
                    return res["url"], "CSV"

            # Si no n'hi ha, aquests datasets pesats sovint venen en ZIP
            for res in resources:
                if res["format"].upper() == "ZIP":
                    return res["url"], "ZIP"

        return None, None
    except Exception as e:
        logging.error(f"Error connectant amb l'API: {e}")
        return None, None

def descarregar_dataframe_bcn(api_url, filtrar_temps=False):
    """Aplica la teva lògica de descàrrega i llegeix per blocs si cal filtrar"""
    download_url, file_format = get_latest_resource_info(api_url)

    if not download_url:
        raise ValueError(f"No s'ha trobat cap recurs CSV ni ZIP a l'URL: {api_url}")

    logging.info("Iniciant descàrrega del contingut...")
    r = requests.get(download_url, headers={'User-Agent': 'Mozilla/5.0'})
    r.raise_for_status()

    if file_format == "ZIP" or download_url.endswith('.zip'):
        logging.info("Format ZIP detectat. Extreient les dades en memòria...")
        with zipfile.ZipFile(BytesIO(r.content)) as z:
            nom_fitxer = z.namelist()[0]
            contingut_cru = z.read(nom_fitxer)
    else:
        contingut_cru = r.content

    if b'\x00' in contingut_cru:
        contingut = contingut_cru.decode('utf-16')
    else:
        contingut = contingut_cru.decode('utf-8', errors='replace')

    csv_data = StringIO(contingut)

    # =========================================================
    # LÒGICA DE LECTURA PER BLOCS (CHUNKING) PER A FITXERS GEGANTS
    # =========================================================
    if filtrar_temps:
        logging.info("Lectura per blocs activada. Buscant la columna de data/hora per filtrar...")
        chunks_filtrats = []

        # Llegim de 250.000 en 250.000 files per no col·lapsar la RAM
        for i, chunk in enumerate(pd.read_csv(csv_data, sep=',', on_bad_lines='skip', engine='python', chunksize=250000)):
            # 1. Netegem les columnes d'aquest bloc petit
            chunk.columns = [str(c).strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in chunk.columns]

            # 2. AUTODETECCIÓ de la columna de temps (només busquem el nom al primer bloc)
            col_temps = next((c for c in chunk.columns if 'time' in c or 'data' in c or 'date' in c or 'timestamp' in c or 'observacio' in c), None)

            if not col_temps:
                if i == 0:
                    logging.error(f"FATAL: No trobo cap columna de temps per filtrar! Les columnes són: {list(chunk.columns)}")
                # Si no hi ha columna, no podem filtrar, així que parem d'afegir escombraries
                continue

            if i == 0:
                logging.info(f"Columna de temps detectada automàticament: '{col_temps}'. Aplicant filtre de l'any...")

            # 3. Convertim a data unificant zones horàries a UTC per evitar errors
            chunk['data_temp'] = pd.to_datetime(chunk[col_temps], errors='coerce', utc=True)

            # Obtenim l'any i el mes actuals del sistema
            any_actual = datetime.now().year


            # FILTRE ESTRICTE: Ens quedem només amb dades d'aquest any I d'aquest mes
            # IMPORTANT: A Pandas, si poses dues condicions, han d'anar entre parèntesis separades per &
            chunk = chunk[chunk['data_temp'].dt.year == any_actual]



            # Esborrem la columna auxiliar per estalviar memòria
            chunk = chunk.drop(columns=['data_temp'])

            # Guardem el bloc ja filtrat
            chunks_filtrats.append(chunk)

        # Unim tots els blocs filtrats en un sol DataFrame (si n'hem trobat algun)
        if chunks_filtrats:
            df = pd.concat(chunks_filtrats, ignore_index=True)
            logging.info(f"Filtratge completat amb èxit! Ens hem quedat amb només {len(df)} files d'aquest any.")
        else:
            logging.warning("El resultat final és de 0 files. Cap dada complia les condicions.")
            df = pd.DataFrame() # Retorna buit si tot ha fallat

    else:
        # Lectura normal per a fitxers petits (com l'inventari d'estacions)
        logging.info("Llegint les dades (lectura normal sense filtres de temps)...")
        df = pd.read_csv(csv_data, sep=',', on_bad_lines='skip', engine='python')
        df.columns = [str(c).strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

    return df


def main():
    logging.info("---------- Actualitzant dades Soroll (Lectures + Estacions) ----------")
    config = get_config()

    # ARA TENIM DUES URLs:
    url_lectures = config.get("opendata.soroll.url")
    url_inventari = config.get("opendata.soroll_estacions.url")
    t_soroll = config.get("db.table.soroll")

    db_user = config.get("spring.datasource.username")
    db_pass = config.get("spring.datasource.password")
    db_url_jdbc = config.get("spring.datasource.url")

    try:
        clean_url = db_url_jdbc.replace("jdbc:", "")
        prefix, rest = clean_url.split("://")
        engine = create_engine(f"{prefix}://{db_user}:{db_pass}@{rest}")
    except Exception as e:
        logging.error(f"Error de connexió a la BD: {e}")
        return

    try:
        # 1. Descarreguem LECTURES (Activarem el filtre de temps perquè és enorme!)
        logging.info(">>> PAS 1: Descarregant LECTURES...")
        df_lectures = descarregar_dataframe_bcn(url_lectures, filtrar_temps=True)

        # 2. Descarreguem INVENTARI D'ESTACIONS (Fitxer petit, lectura normal)
        logging.info(">>> PAS 2: Descarregant INVENTARI D'ESTACIONS...")
        df_inventari = descarregar_dataframe_bcn(url_inventari, filtrar_temps=False)

        # 2. Comprovem la columna pont (id_instal)
        # El teu codi de neteja haurà convertit "Id_Instal" en "id_instal" automàticament!
        if 'id_instal' not in df_lectures.columns or 'id_instal' not in df_inventari.columns:
            logging.error("No es troba la columna 'id_instal' per fer el creuament.")
            logging.error(f"Lectures: {list(df_lectures.columns)}")
            logging.error(f"Inventari: {list(df_inventari.columns)}")
            return

        # 3. Creuament de dades
        logging.info(">>> PAS 3: Creuant dades per 'id_instal'...")
        df_merged = pd.merge(df_lectures, df_inventari, on='id_instal', how='inner')

        # 4. Extracció de coordenades i decibels
        col_val = 'valor' if 'valor' in df_merged.columns else next((c for c in df_merged.columns if 'laeq' in c), None)

        if not col_val or 'latitud' not in df_merged.columns or 'longitud' not in df_merged.columns:
            logging.error("No s'han trobat les columnes de valor, latitud o longitud després del creuament.")
            return

        # Arreglem comes per punts i passem a numèric
        for col in [col_val, 'latitud', 'longitud']:
            df_merged[col] = pd.to_numeric(df_merged[col].astype(str).str.replace(',', '.'), errors='coerce')

        # Agrupem per punt geogràfic i fem la mitjana
        df_final = df_merged.dropna(subset=[col_val, 'latitud', 'longitud'])
        df_final = df_final.groupby(['latitud', 'longitud'])[col_val].mean().reset_index()
        df_final.columns = ['latitud', 'longitud', 'nivell_db']

        # 5. Pugem a PostgreSQL
        logging.info(f">>> PAS 4: Pujant {len(df_final)} sensors geolocalitzats a la taula '{t_soroll}'...")
        with engine.begin() as conn:
            conn.execute(text(f'DROP TABLE IF EXISTS "{t_soroll}" CASCADE;'))

        df_final.to_sql(t_soroll, engine, if_exists='replace', index=False)
        logging.info(f"ÈXIT: Taula '{t_soroll}' actualitzada correctament amb les dades de soroll enriquides.")

    except Exception as e:
        logging.error(f"Error crític en el processament: {e}")

if __name__ == "__main__":
    main()