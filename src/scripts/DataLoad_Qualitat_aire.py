import os
import sys
import requests
import pandas as pd
import logging
from datetime import datetime
from sqlalchemy import create_engine, text
from io import StringIO, BytesIO
import zipfile

# --- CONFIGURACIO DELS PATHS I LOGS ---
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
APP_PROPERTIES_PATH = os.path.normpath(os.path.join(BASE_DIR, "..", "main", "resources", "application.properties"))
APP_LOCAL_PROPERTIES_PATH = os.path.normpath(os.path.join(BASE_DIR, "..", "main", "resources", "application-local.properties"))
LOGS_DIR = os.path.join(BASE_DIR, "logs")

if not os.path.exists(LOGS_DIR):
    os.makedirs(LOGS_DIR)

log_filename = os.path.join(LOGS_DIR, f"{datetime.now().strftime('%Y-%m-%d_%H-%M-%S')}_dataload-qualitat-aire.log")

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
    local_path = sys.argv[1] if len(sys.argv) > 1 else APP_LOCAL_PROPERTIES_PATH
    config = load_properties(local_path, config)
    return config

def get_latest_resource_info(api_url):
    logging.info(f"Consultant l'API per: {api_url}")
    try:
        headers = {'User-Agent': 'Mozilla/5.0'}
        response = requests.get(api_url, headers=headers)
        response.raise_for_status()
        data = response.json()
        if data.get("success"):
            resources = data["result"]["resources"]
            for res in resources:
                if res["format"].upper() == "CSV": return res["url"], "CSV"
            for res in resources:
                if res["format"].upper() == "ZIP": return res["url"], "ZIP"
        return None, None
    except Exception as e:
        logging.error(f"Error connectant amb l'API: {e}")
        return None, None

def descarregar_dataframe(api_url):
    download_url, file_format = get_latest_resource_info(api_url)
    if not download_url:
        raise ValueError(f"No s'ha trobat recurs per a: {api_url}")

    logging.info(f"Descarregant des de {download_url}...")
    r = requests.get(download_url, headers={'User-Agent': 'Mozilla/5.0'})
    r.raise_for_status()

    if file_format == "ZIP" or download_url.endswith('.zip'):
        with zipfile.ZipFile(BytesIO(r.content)) as z:
            nom_fitxer = z.namelist()[0]
            contingut = z.read(nom_fitxer).decode('utf-8', errors='replace')
    else:
        contingut = r.content.decode('utf-8', errors='replace')

    df = pd.read_csv(StringIO(contingut), sep=',', engine='python')
    df.columns = [c.strip().lower().replace(' ', '_').replace('.', '') for c in df.columns]
    return df

def main():
    logging.info("---------- Actualitzant Qualitat de l'Aire (Temps Real) ----------")
    config = get_config()

    url_lectures = config.get("opendata.aire.url")
    url_inventari = config.get("opendata.aire_estacions.url")
    t_qualitat_aire = config.get("db.table.qualitat_aire")

    db_user = config.get("spring.datasource.username")
    db_pass = config.get("spring.datasource.password")
    db_url_jdbc = config.get("spring.datasource.url")

    if not db_pass:
        logging.error("ATENCIÓ: La contrasenya és NULL!")
        return

    try:
        clean_url = db_url_jdbc.replace("jdbc:", "")
        prefix, rest = clean_url.split("://")
        engine = create_engine(f"{prefix}://{db_user}:{db_pass}@{rest}")

        df_lectures = descarregar_dataframe(url_lectures)
        df_inventari = descarregar_dataframe(url_inventari)

        col_estacio_lec = next((c for c in df_lectures.columns if 'estaci' in c), None)
        col_estacio_inv = next((c for c in df_inventari.columns if 'estaci' in c), None)

        if not col_estacio_lec or not col_estacio_inv:
            logging.error(f"Falta columna estació.")
            return

        df_merged = pd.merge(df_lectures, df_inventari, left_on=col_estacio_lec, right_on=col_estacio_inv, how='inner')

        # --- 1. FILTRATGE PER NO2 (Codi 9)
        col_contaminant = next((c for c in df_merged.columns if 'contaminant' in c or 'element' in c), None)
        if col_contaminant:
            logging.info(f"Filtrant dades pel contaminant PM2.5 (Codi 9)...")
            df_merged = df_merged[df_merged[col_contaminant].astype(str) == '9']

        if df_merged.empty:
            logging.error("El creuament o el filtre de contaminant ha deixat la taula buida.")
            return

        # --- 2. IDENTIFICACIÓ DE COLUMNES DE MESURA <<-- AQUÍ ---
        # Busquem h01, h02... fins h24
        columnes_hora = [c for c in df_merged.columns if (c.startswith('h') and c[1:].isdigit())]

        if not columnes_hora:
            col_val = next((c for c in df_merged.columns if 'valor' in c or 'concentraci' in c), None)
            columnes_a_mitjanar = [col_val] if col_val else []
        else:
            columnes_a_mitjanar = columnes_hora

        col_lat = next((c for c in df_merged.columns if 'lat' in c), None)
        col_lon = next((c for c in df_merged.columns if 'lon' in c), None)

        # --- 3. CÀLCUL DE LA MITJANA I NETEJA <<-- AQUÍ ---
        for col in columnes_a_mitjanar + [col_lat, col_lon]:
            df_merged[col] = pd.to_numeric(df_merged[col].astype(str).str.replace(',', '.'), errors='coerce')

        # Calculem la mitjana horitzontal (la de les columnes h01-h24)
        df_merged['valor_final'] = df_merged[columnes_a_mitjanar].mean(axis=1)

        # Filtre de seguretat per evitar IDs d'estació o errors
        df_merged = df_merged[df_merged['valor_final'] <= 250]

        # Ens quedem només on tinguem número
        df_final = df_merged.dropna(subset=['valor_final', col_lat, col_lon])

        # Agrupem per estació física i fem la mitjana final
        df_final = df_final.groupby([col_lat, col_lon])['valor_final'].mean().reset_index()
        df_final.columns = ['latitud', 'longitud', 'nivell_aire']

        # Pujada a SQL
        logging.info(f"Pujant {len(df_final)} estacions amb dades de PM2.5 a {t_qualitat_aire}...")
        with engine.begin() as conn:
            conn.execute(text(f'TRUNCATE TABLE "{t_qualitat_aire}" RESTART IDENTITY CASCADE;'))
        df_final.to_sql(t_qualitat_aire, engine, if_exists='append', index=False)
        logging.info("ÈXIT TOTAL: Dades filtrades i netejades correctament.")

    except Exception as e:
        logging.error(f"Error procés: {e}")

if __name__ == "__main__":
    main()