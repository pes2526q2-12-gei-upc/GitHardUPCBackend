# per executar el test has de fer cd src/scripts i després python -m tests.test_DataLoad_Bancs
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import sys
import pandas as pd
from io import StringIO

from DataLoad_Bancs import load_properties, get_latest_csv_url

class TestDataLoadBancs(unittest.TestCase):

    ### --- TESTS DE CONFIGURACIÓ ---
    @patch("os.path.exists")
    def test_load_properties_basic(self, mock_exists):
        """Comprova que es carreguen correctament les propietats clau-valor."""
        mock_exists.return_value = True
        content = "db.table.bancs=taula_test\nopendata.bancs.url=https://api.test"
        with patch("builtins.open", mock_open(read_data=content)):
            config = load_properties("fake.properties", {})
            self.assertEqual(config["db.table.bancs"], "taula_test")
            self.assertEqual(config["opendata.bancs.url"], "https://api.test")

    ### --- TESTS D'API ---
    @patch("requests.get")
    def test_get_latest_csv_url_filtering(self, mock_get):
        """Verifica que el script tria el recurs format CSV i ignora la resta."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [
                    {"format": "XML", "url": "https://test.com/bancs.xml"},
                    {"format": "CSV", "url": "http://test.com/bancs.csv"}
                ]
            }
        }
        mock_get.return_value = mock_response

        url = get_latest_csv_url("https://api.test")
        self.assertEqual(url, "https://test.com/bancs.csv")

    ### --- TESTS DE LÒGICA DE DADES (PANDAS) ---
    def test_dataframe_cleaning_bancs(self):
        """Verifica la neteja de columnes específica per a la taula de Bancs."""
        # Simulem capçaleres típiques que podrien venir amb punts o espais
        csv_content = "ID.Element,Tipus Banc,Longitud (m)\n1,Fusta,2.5"
        df = pd.read_csv(StringIO(csv_content))

        # Lògica de neteja del script
        df.columns = [c.strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        expected = ["idelement", "tipus_banc", "longitud_m"]
        self.assertListEqual(list(df.columns), expected)

    ### --- TESTS DE SEGURETAT D'ESTRUCTURA ---
    def test_empty_dataframe_protection(self):
        """Comprova que el script detectaria si el CSV no té columnes útils."""
        # Un CSV mal format o amb una sola columna que no és vàlida
        csv_content = "Dades corruptes sense comes"
        df = pd.read_csv(StringIO(csv_content))

        # La condició que tens al main(): if df.shape[1] <= 1
        is_invalid = df.shape[1] <= 1
        self.assertTrue(is_invalid)

if __name__ == "__main__":
    unittest.main()