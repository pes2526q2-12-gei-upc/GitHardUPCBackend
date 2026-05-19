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
    """Llegeix les propietats dels fitxers .properties i resol variables d'entorn"""
    if not os.path.exists(file_path):
        return current_config
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if "=" in line and not line.startswith("#"):
                    key, value = line.split("=", 1)
                    val = value.strip()

                    if val.startswith("${") and val.endswith("}"):
                        var_name = val[2:-1].split(":")[0]
                        val = os.environ.get(var_name, val)
                    current_config[key.strip()] = val
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

        headers = {
            'User-Agent': 'Mozilla/5.0',
            'Accept': 'text/csv,*/*;q=0.8'
        }

        r = requests.get(url, headers=headers, timeout=60)
        r.raise_for_status()

        # Tractament d'encoding
        if b'\x00' in r.content:
            logging.info("Format UTF-16 detectat. Decodificant...")
            contingut = r.content.decode('utf-16')
        else:
            contingut = r.content.decode('utf-8', errors='replace')

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
            .replace('ó', 'o').replace('í', 'i').replace('á', 'a')
            .replace('é', 'e').replace('ú', 'u').replace('à', 'a')
            .replace('è', 'e').replace('ò', 'o')
            for c in df.columns
        ]

        logging.info(f"Dades originals: {len(df)} files.")

        # =========================================================================
        # TRACTAMENT DE DADES: FILTRATGE I AGRUPACIÓ PER ABP (DISTRICTES BCN)
        # =========================================================================

        # 1. Filtrar només per la Regió Policial de Barcelona
        df = df[df['regio_policial_rp'].str.contains('Metropolitana Barcelona', case=False, na=False)]

        # 2. Extreure el nom del districte traient el prefix "ABP "
        df['nom_districte'] = df['area_basica_policial_abp'].str.replace('ABP ', '', case=False, regex=True).str.strip()

        # 3. Eliminar columnes innecessàries (INCLOENT ANY I MES) per fer la BD més neta
        # Eliminem tot allò que faria que les files no s'ajuntessin
        columnes_a_eliminar = [
            'regio_policial_rp', 'area_basica_policial_abp',
            'codi_comarca', 'nom_comarca', 'codi_provincia', 'nom_provincia',
            'any', 'mes', 'num_mes' # <-- Afegim les temporals aquí perquè desapareguin
        ]
        df = df.drop(columns=[col for col in columnes_a_eliminar if col in df.columns], errors='ignore')

        # 4. Agrupar ÚNICAMENT per districte per tenir el total absolut
        df = df.groupby(['nom_districte'], as_index=False).sum(numeric_only=True)

        # =========================================================================

        logging.info(f"Dades després de filtrar per Barcelona i agrupar per ABP: {len(df)} files.")

        # Actualització a la BD
        with engine.begin() as conn:
            conn.execute(text(f'TRUNCATE TABLE "{t_infraccions}" RESTART IDENTITY CASCADE;'))

        df.to_sql(t_infraccions, engine, if_exists='append', index=False)

        logging.info(f"Procés completat amb èxit! Taula '{t_infraccions}' creada.")

    except Exception as e:
        logging.error(f"S'ha produït un error durant el procés: {e}")