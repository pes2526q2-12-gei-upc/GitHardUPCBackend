# per executar el test has de fer cd src/scripts i després python -m tests.test_DataLoad_Refugis_climatics
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import pandas as pd
import zipfile
from io import StringIO, BytesIO

from DataLoad_Refugis_climatics import load_properties, get_latest_resource_info

class TestDataLoadRefugisClimatics(unittest.TestCase):

    ### --- TESTS DE CONFIGURACIÓ ---
    @patch("os.path.exists")
    def test_load_properties_refugis(self, mock_exists):
        """Verifica que es carreguen les propietats de la taula de refugis."""
        mock_exists.return_value = True
        content = "db.table.refugis=bcn_refugis_climatics\nopendata.refugis.url=https://api.refugis"
        with patch("builtins.open", mock_open(read_data=content)):
            config = load_properties("fake.properties", {})
            self.assertEqual(config["db.table.refugis"], "bcn_refugis_climatics")
            self.assertEqual(config["opendata.refugis.url"], "https://api.refugis")

    ### --- TESTS D'API (SUPORT ZIP/CSV) ---
    @patch("requests.get")
    def test_get_latest_resource_info_csv(self, mock_get):
        """Verifica que troba el recurs si és un CSV directe."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [{"format": "CSV", "url": "https://opendata.bcn/refugis.csv"}]
            }
        }
        mock_get.return_value = mock_response
        url, fmt = get_latest_resource_info("https://api.test")
        self.assertEqual(url, "https://opendata.bcn/refugis.csv")
        self.assertEqual(fmt, "CSV")

    @patch("requests.get")
    def test_get_latest_resource_info_zip(self, mock_get):
        """Verifica que troba el recurs si és un ZIP."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [{"format": "ZIP", "url": "https://opendata.bcn/refugis.zip"}]
            }
        }
        mock_get.return_value = mock_response
        url, fmt = get_latest_resource_info("https://api.test")
        self.assertEqual(url, "https://opendata.bcn/refugis.zip")
        self.assertEqual(fmt, "ZIP")

    ### --- TESTS DE LÒGICA DE DADES (PANDAS) ---
    def test_dataframe_cleaning_refugis(self):
        """Verifica la neteja de columnes típica per a PostgreSQL."""
        df = pd.DataFrame(columns=["ID.Refugi", "Nom (Espai)", "Adreça.Carrer"])
        cleaned_columns = [str(c).strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]
        expected = ["idrefugi", "nom_espai", "adreçacarrer"]
        self.assertListEqual(cleaned_columns, expected)

    ### --- TESTS DE FORMAT D'ARXIU ---
    def test_zip_extraction_simulation(self):
        """Simula l'extracció d'un CSV des d'un ZIP en memòria."""
        buf = BytesIO()
        with zipfile.ZipFile(buf, 'w') as z:
            z.writestr("dades.csv", "id,nom\n1,Biblioteca")

        with zipfile.ZipFile(BytesIO(buf.getvalue())) as z:
            nom_fitxer = z.namelist()[0]
            contingut = z.read(nom_fitxer).decode('utf-8')

        self.assertIn("id,nom", contingut)

if __name__ == "__main__":
    unittest.main()