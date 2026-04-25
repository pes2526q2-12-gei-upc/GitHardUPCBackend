# per executar el test has de fer cd src/scripts i després python -m tests.test_DataLoad_Qualitat_aire
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import pandas as pd
import zipfile
from io import StringIO, BytesIO
import urllib.parse

# Importem les funcions del nou script d'Aire
from DataLoad_Qualitat_aire import load_properties, get_latest_resource_info

class TestDataLoadQualitatAire(unittest.TestCase):

    ### --- TESTS DE CONFIGURACIÓ I VARIABLES D'ENTORN ---
    @patch("os.path.exists")
    def test_load_properties_qualitat_aire(self, mock_exists):
        """Verifica que es carreguen les propietats bàsiques."""
        mock_exists.return_value = True
        content = "db.table.qualitat_aire=bcn_qualitat_aire\nopendata.aire.url=https://api.aire"
        with patch("builtins.open", mock_open(read_data=content)):
            config = load_properties("fake.properties", {})
            self.assertEqual(config["db.table.qualitat_aire"], "bcn_qualitat_aire")
            self.assertEqual(config["opendata.aire.url"], "https://api.aire")

    def test_load_properties_with_env_vars(self):
        """Verifica que la contrasenya oculta amb ${VAR} es resol correctament."""
        content = "spring.datasource.password=${SAFESTEPS_DB_PASSWORD}"
        with patch("builtins.open", mock_open(read_data=content)):
            with patch("os.path.exists", return_value=True):
                with patch.dict(os.environ, {"SAFESTEPS_DB_PASSWORD": "password_super_secreta!"}):
                    config = {}
                    result = load_properties("fake.properties", config)
                    self.assertEqual(result["spring.datasource.password"], "password_super_secreta!")

    def test_password_encoding(self):
        """Verifica que caràcters especials en la contrasenya es codifiquen bé per a SQLAlchemy."""
        password_original = "admin@123/?"
        password_safe = urllib.parse.quote_plus(password_original)
        self.assertEqual(password_safe, "admin%40123%2F%3F")
        self.assertNotIn("@", password_safe, "La @ ha d'estar codificada per no trencar la connexió")

    ### --- TESTS D'API (SUPORT ZIP I CSV) ---
    @patch("requests.get")
    def test_get_latest_resource_info_csv(self, mock_get):
        """Simula una resposta on troba directament un CSV."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [
                    {"format": "CSV", "url": "https://opendata.bcn/aire.csv"}
                ]
            }
        }
        mock_get.return_value = mock_response

        url, fmt = get_latest_resource_info("https://api.test")
        self.assertEqual(url, "https://opendata.bcn/aire.csv")
        self.assertEqual(fmt, "CSV")

    @patch("requests.get")
    def test_get_latest_resource_info_zip(self, mock_get):
        """Verifica que si no hi ha CSV però hi ha ZIP, agafa el ZIP."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [
                    {"format": "ZIP", "url": "https://opendata.bcn/aire.zip"}
                ]
            }
        }
        mock_get.return_value = mock_response

        url, fmt = get_latest_resource_info("https://api.test")
        self.assertEqual(url, "https://opendata.bcn/aire.zip")
        self.assertEqual(fmt, "ZIP")

    ### --- TESTS DE LÒGICA DE DADES (PANDAS) ---
    def test_neteja_columnes(self):
        """Verifica la neteja de columnes (espais, majúscules i punts)."""
        df = pd.DataFrame(columns=["Nom.Estació", "Valor (H01)", "Latitud "])
        df.columns = [c.strip().lower().replace(' ', '_').replace('.', '') for c in df.columns]

        expected = ["nomestació", "valor_(h01)", "latitud"]
        self.assertListEqual(list(df.columns), expected)

    def test_creuament_i_agregacio_aire(self):
        """Prova el cor de l'script: Inner Join, neteja de comes decimals i càlcul de la mitjana."""
        # 1. Simulem les dades de lectures (Amb comes als decimals com passa a l'OpenData!)
        df_lectures = pd.DataFrame({
            "estacio": [4, 4, 11],
            "valor": ["45,5", "55,0", "10,2"] # Simulem string amb coma
        })

        # 2. Simulem les dades d'inventari d'estacions (Ubicacions)
        df_inventari = pd.DataFrame({
            "estacio": [4, 11, 99], # L'estació 99 està "trencada" i no té lectures
            "latitud": ["41,38", "41,40", "41,00"],
            "longitud": ["2,16", "2,18", "2,00"]
        })

        # 3. Lògica del main() replicada:
        # Join
        df_merged = pd.merge(df_lectures, df_inventari, on="estacio", how="inner")

        # Conversió a numèric (Reemplaçant comes)
        for col in ["valor", "latitud", "longitud"]:
            df_merged[col] = pd.to_numeric(df_merged[col].astype(str).str.replace(',', '.'), errors='coerce')

        # Agregació
        df_final = df_merged.dropna(subset=["valor", "latitud", "longitud"])
        df_final = df_final.groupby(["latitud", "longitud"])["valor"].mean().reset_index()

        # --- COMPROVACIONS ---
        # L'estació 99 ha de desaparèixer, només queden la 4 i la 11
        self.assertEqual(len(df_final), 2)

        # La mitjana de l'estació 4 (45.5 i 55.0) ha de ser 50.25
        mitjana_estacio_4 = df_final[df_final["latitud"] == 41.38]["valor"].iloc[0]
        self.assertEqual(mitjana_estacio_4, 50.25)

        # La coordenada 41,38 ha de passar a 41.38 format float
        self.assertTrue(isinstance(df_final["latitud"].iloc[0], float))

if __name__ == "__main__":
    unittest.main()