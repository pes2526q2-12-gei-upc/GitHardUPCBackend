# per executar el test has de fer cd src/scripts i després python -m tests.test_DataLoad_Arbrat_zona
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import sys
import pandas as pd
from io import StringIO

from DataLoad_Arbrat_zona import load_properties, get_latest_csv_url

class TestDataLoadArbratZona(unittest.TestCase):

    ### --- TESTS DE CONFIGURACIÓ ---
    @patch("os.path.exists")
    def test_load_properties_missing_file(self, mock_exists):
        """Verifica que si el fitxer no existeix, el diccionari no canvia."""
        mock_exists.return_value = False
        config = {"clau": "valor_original"}
        result = load_properties("no_existeix.properties", config)
        self.assertEqual(result["clau"], "valor_original")

    def test_load_properties_with_env_vars(self):
        """Verifica que les variables tipus ${VAR} es llegeixen de l'entorn."""
        content = "db.password=${MY_DB_PASS}"
        with patch("builtins.open", mock_open(read_data=content)):
            with patch("os.path.exists", return_value=True):
                with patch.dict(os.environ, {"MY_DB_PASS": "safe_testing_password_123"}):
                    config = {}
                    result = load_properties("fake.properties", config)
                    self.assertEqual(result["db.password"], "secret123")

    ### --- TESTS D'API ---
    @patch("requests.get")
    def test_get_latest_csv_url_success(self, mock_get):
        """Simula una resposta correcta de l'API d'Open Data."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [
                    {"format": "PDF", "url": "https://test.com/doc.pdf"},
                    {"format": "CSV", "url": "https://test.com/dades_zona.csv"}
                ]
            }
        }
        mock_get.return_value = mock_response

        url = get_latest_csv_url("https://api.test")
        self.assertEqual(url, "https://test.com/dades_zona.csv")

    ### --- TESTS DE LÒGICA DE DADES (PANDAS) ---
    def test_dataframe_cleaning_logic(self):
        """Verifica que els noms de les columnes es netegen correctament per a la BD."""
        csv_content = "Codi.Zona,Nom (Zona),Superfície m2\n123,Eixample,50.5"
        df = pd.read_csv(StringIO(csv_content))

        # Repliquem la lògica de neteja del script original
        df.columns = [c.strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        expected_columns = ["codizona", "nom_zona", "superfície_m2"]
        self.assertListEqual(list(df.columns), expected_columns)

    def test_encoding_detection_logic(self):
        """Verifica la detecció de caràcters nuls per a format UTF-16."""
        # Contingut simulat en UTF-16 (tindrà bytes nuls)
        contingut_binari = "col1,col2".encode('utf-16')

        # Simulem el bloc de decodificació del main
        if b'\x00' in contingut_binari:
            contingut_decodificat = contingut_binari.decode('utf-16')
        else:
            contingut_decodificat = contingut_binari.decode('utf-8')

        self.assertEqual(contingut_decodificat, "col1,col2")

if __name__ == "__main__":
    unittest.main()