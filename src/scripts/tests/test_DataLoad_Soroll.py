# per executar el test has de fer cd src/scripts i després python -m tests.test_DataLoad_Soroll
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import pandas as pd
import zipfile
from io import StringIO, BytesIO

from DataLoad_Soroll import load_properties, get_latest_resource_info

class TestDataLoadSoroll(unittest.TestCase):

    ### --- TESTS DE CONFIGURACIÓ ---
    @patch("os.path.exists")
    def test_load_properties_soroll(self, mock_exists):
        """Verifica que es carreguen les propietats de la taula de soroll."""
        mock_exists.return_value = True
        content = "db.table.soroll=bcn_contaminacio_acustica\nopendata.soroll.url=https://api.soroll"
        with patch("builtins.open", mock_open(read_data=content)):
            config = load_properties("fake.properties", {})
            self.assertEqual(config["db.table.soroll"], "bcn_contaminacio_acustica")
            self.assertEqual(config["opendata.soroll.url"], "https://api.soroll")

    ### --- TESTS D'API (SUPORT ZIP/CSV) ---
    @patch("requests.get")
    def test_get_latest_resource_info_csv(self, mock_get):
        """Verifica que troba el recurs si és un CSV directe."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [{"format": "CSV", "url": "https://opendata.bcn/soroll.csv"}]
            }
        }
        mock_get.return_value = mock_response
        url, fmt = get_latest_resource_info("https://api.test")
        self.assertEqual(url, "https://opendata.bcn/soroll.csv")
        self.assertEqual(fmt, "CSV")

    @patch("requests.get")
    def test_get_latest_resource_info_zip(self, mock_get):
        """Verifica que troba el recurs si és un ZIP (com passa amb el soroll)."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [{"format": "ZIP", "url": "https://opendata.bcn/soroll.zip"}]
            }
        }
        mock_get.return_value = mock_response
        url, fmt = get_latest_resource_info("https://api.test")
        self.assertEqual(url, "https://opendata.bcn/soroll.zip")
        self.assertEqual(fmt, "ZIP")

    ### --- TESTS DE LÒGICA DE DADES (PANDAS) ---
    def test_dataframe_cleaning_soroll(self):
        """Verifica la neteja de columnes típica per a PostgreSQL."""
        df = pd.DataFrame(columns=["ID.Sensor", "Nom (Carrer)", "Valor.Soroll"])
        cleaned_columns = [str(c).strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]
        expected = ["idsensor", "nom_carrer", "valorsoroll"]
        self.assertListEqual(cleaned_columns, expected)

    ### --- TESTS DE FORMAT D'ARXIU ---
    def test_zip_extraction_simulation(self):
        """Simula l'extracció d'un CSV des d'un ZIP en memòria."""
        buf = BytesIO()
        with zipfile.ZipFile(buf, 'w') as z:
            z.writestr("dades.csv", "sensor,valor\n1,65")

        with zipfile.ZipFile(BytesIO(buf.getvalue())) as z:
            nom_fitxer = z.namelist()[0]
            contingut = z.read(nom_fitxer).decode('utf-8')

        self.assertIn("sensor,valor", contingut)

if __name__ == "__main__":
    unittest.main()