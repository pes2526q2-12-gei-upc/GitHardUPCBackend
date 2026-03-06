import os
import requests
import zipfile
import io
import pandas as pd
from sqlalchemy import create_engine

#Per executar:
#set SAFESTEPS_DB_PASSWORD=LACONTRASSENYA && python DataLoad_GrafViari.py

URL_ZIP = "https://opendata-ajuntament.barcelona.cat/data/dataset/mapa-graf-viari-carrers-wms/resource/d0ca0925-3e38-49a3-91d4-f7feb23f8efc/download"

def main():
    print("Descarregant dades des de l'Open Data BCN...")
    #Descarrega de dades de l'API
    response = requests.get(URL_ZIP)

    if response.status_code != 200:
        print("Error en descarregar el fitxer")
        return


    #Connexió a la base de dades PostgreSQL
    print("Connectant a la base de dades PostgreSQL...")
    DB_USER = "admin_safesteps"
    DB_PASS = os.environ.get("SAFESTEPS_DB_PASSWORD")
    if DB_PASS:
        DB_PASS = DB_PASS.strip()


    if not DB_PASS:
        print("ERROR: No s'ha trobat la variable d'entorn 'SAFESTEPS_DB_PASSWORD'.")
        return

    DB_HOST = "nattech.fib.upc.edu"
    DB_PORT = "40390"
    DB_NAME = "safesteps"

    engine_url = f"postgresql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
    engine = create_engine(engine_url)


    #Lectura de dades
    print("Obrint el fitxer ZIP...")
    zip_file = zipfile.ZipFile(io.BytesIO(response.content))

    arxius = zip_file.namelist()
    print(f"\nARXIUS TROBATS AL ZIP: {arxius}\n")

    for filename in arxius:
        # Només volem els CSV
        if filename.lower().endswith('.csv'):
            print(f"Processant l'arxiu: {filename}...")

            try:
                with zip_file.open(filename) as f:
                    df = pd.read_csv(f, sep=';', encoding='latin1', low_memory=False)

                #Nom de les taules de dades, canviar a posteriori.
                table_name = filename.split('.')[0].lower()
                table_name = "".join(c if c.isalnum() else '_' for c in table_name)

                df.to_sql(table_name, engine, if_exists='replace', index=False)

            except Exception as e:
                print(f"L'arxiu {filename} no s'ha pogut processar. Error: {e}\n")

    print("Done!")

if __name__ == "__main__":
    main()