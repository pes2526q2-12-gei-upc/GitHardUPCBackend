# per executar el test has de fer cd src/scripts/tests i després python -m unittest test_DataLoad_Arbrat_viari.py
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import pandas as pd
from io import StringIO

from DataLoad_Arbrat_viari import load_properties, get_latest_csv_url

class TestDataloadArbrat(unittest.TestCase):

    ## --- Tests per a load_properties ---
    @patch("os.path.exists")
    def test_load_properties_file_not_found(self, mock_exists):
        mock_exists.return_value = False
        config = {"original": "valor"}
        result = load_properties("no_existeix.properties", config)
        self.assertEqual(result, {"original": "valor"})

    def test_load_properties_parsing(self):
        content = "db.user=admin\n#comentari=ignora\napi.url=https://test.com\n"
        with patch("builtins.open", mock_open(read_data=content)):
            with patch("os.path.exists", return_value=True):
                config = {}
                result = load_properties("fake.properties", config)
                self.assertEqual(result["db.user"], "admin")
                self.assertEqual(result["api.url"], "https://test.com")
                self.assertNotIn("#comentari", result)

    ## --- Tests per a get_latest_csv_url ---
    @patch("requests.get")
    def test_get_latest_csv_url_success(self, mock_get):
        # Simulem una resposta JSON de l'API de CKAN/OpenData
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [
                    {"format": "JSON", "url": "https://test.com/data.json"},
                    {"format": "CSV", "url": "https://test.com/data.csv"}
                ]
            }
        }
        mock_get.return_value = mock_response

        url = get_latest_csv_url("https://api.fake")
        self.assertEqual(url, "https://test.com/data.csv")

    @patch("requests.get")
    def test_get_latest_csv_url_api_error(self, mock_get):
        mock_get.side_effect = Exception("Connexió fallida")
        url = get_latest_csv_url("https://api.fake")
        self.assertIsNone(url)

    ## --- Test de processament de dades (Lògica de Pandas) ---
    # Aquest test verifica que la neteja de columnes que fas al main() funciona bé
    def test_dataframe_column_cleaning(self):
        # Simulem un CSV amb noms bruts
        csv_content = "ID.Node,Nom (Comú),Codi.Barri\n1,Roure,10"
        df = pd.read_csv(StringIO(csv_content))

        # Repliquem la teva lògica de neteja
        df.columns = [c.strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        expected_columns = ["idnode", "nom_comú", "codibarri"]
        self.assertListEqual(list(df.columns), expected_columns)

if __name__ == "__main__":
    unittest.main()