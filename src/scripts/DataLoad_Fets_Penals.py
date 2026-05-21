import os
import sys
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

# Configuració segura del Logger
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
    local_path = sys.argv[1] if len(sys.argv) > 1 else APP_LOCAL_PROPERTIES_PATH
    config = load_properties(local_path, config)
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

        # Neteja forta de columnes (llevem espais, parèntesis i accents per evitar problemes amb Pandas)
        df.columns = [
            str(c).strip().lower()
            .replace(' ', '_').replace('.', '').replace('(', '').replace(')', '')
            .replace('à', 'a').replace('è', 'e').replace('é', 'e').replace('í', 'i').replace('ò', 'o').replace('ó', 'o').replace('ú', 'u')
            for c in df.columns
        ]

        logger.info(f"Dades originals descarregades: {len(df)} files.")

        # =====================================================================
        # PAS 1: FILTRATGE I AGREGACIÓ (PANDAS)
        # =====================================================================

        # A. Filtrar pel últim any (evitem històrics que no reflecteixen la ciutat actual)
        max_year = df['any'].max()
        df = df[df['any'] == max_year]

        # B. Filtrar només els districtes de Barcelona ciutat
        bcn_abps = [
            'ABP Sant Martí', 'ABP Ciutat Vella', 'ABP Eixample', 'ABP Sants-Montjuïc',
            'ABP Les Corts', 'ABP Sarrià-Sant Gervasi', 'ABP Gràcia', 'ABP Horta-Guinardó',
            'ABP Nou Barris', 'ABP Sant Andreu'
        ]
        df = df[df['area_basica_policial_abp'].isin(bcn_abps)]
        logger.info(f"Després de filtrar per ABP (Barcelona): {len(df)} files.")

        # C. Filtrar pels delictes que importen a un vianant (mitjançant expressions regulars)
        paraules_clau_perill = 'violència|lesions|sexual|homicidi|amenaces|coaccions|robatori amb força|estrebada'
        df = df[df['tipus_de_fet'].str.contains(paraules_clau_perill, case=False, na=False)]
        logger.info(f"Després de filtrar per tipus de delicte: {len(df)} files.")

        # D. Agrupar i sumar els fets coneguts per cada districte
        df_agrupat = df.groupby('area_basica_policial_abp')['coneguts'].sum().reset_index()

        # E. Formatejar les dades finals per facilitar el creuament amb els mapes
        # Llevem el text "ABP " perquè ens quedi només "Eixample", "Ciutat Vella", etc.
        df_agrupat['area_basica_policial_abp'] = df_agrupat['area_basica_policial_abp'].str.replace('ABP ', '', regex=False)

        # Renombrem les columnes perquè la taula final sigui neta i explícita
        df_agrupat = df_agrupat.rename(columns={
            'area_basica_policial_abp': 'nom_districte',
            'coneguts': 'total_delictes_vianants'
        })

        # =====================================================================

        logger.info("Dades filtrades i agrupades. Pujant %s districtes a la taula '%s'...", len(df_agrupat), t_fets)
        with engine.begin() as conn:
            conn.execute(text(f'TRUNCATE TABLE "{t_fets}" RESTART IDENTITY CASCADE;'))

        # Pugem el DataFrame AGRUPAT, no l'original
        df_agrupat.to_sql(t_fets, engine, if_exists='append', index=False)

        logger.info("ÈXIT: Taula '%s' actualitzada correctament només amb els districtes de Barcelona i delictes rellevants.", t_fets)

    except Exception as e:
        logger.error("Error en el processament: %s", e)
        import sys
        sys.exit(1)  # ¡Le decimos a Java que ESTO HA FALLADO!

if __name__ == "__main__":
    main()