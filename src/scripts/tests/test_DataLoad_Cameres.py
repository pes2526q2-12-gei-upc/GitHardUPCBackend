# per executar el test has de fer cd src/scripts i després python -m tests.test_DataLoad_Cameres
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import sys
import pandas as pd
import zipfile
from io import BytesIO

from DataLoad_Cameres import load_properties, get_latest_resource_info, read_data_into_dataframe

class TestDataLoadCameres(unittest.TestCase):

    ### --- TESTS D'API (MULTI-FORMAT) ---
    @patch("requests.get")
    def test_get_latest_resource_info_priority(self, mock_get):
        """Verifica que el script prioritza formats segons la llista permesa."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [
                    {"format": "HTML", "url": "https://test.com/web"},
                    {"format": "ZIP", "url": "https://test.com/data.zip"},
                    {"format": "CSV", "url": "https://test.com/data.csv"}
                ]
            }
        }
        mock_get.return_value = mock_response

        # Segons el teu codi, el primer format de la llista ["CSV", "JSON", "ZIP"] que trobi guanya
        url, fmt = get_latest_resource_info("https://api.test")
        self.assertEqual(fmt, "CSV")
        self.assertEqual(url, "https://test.com/data.csv")

    ### --- TESTS DE PROCESSAMENT DE FORMATS (read_data_into_dataframe) ---

    def test_read_csv_content_utf8(self):
        """Comprova la lectura de CSV estàndard."""
        contingut = "id,nom\n1,camera1".encode('utf-8')
        df = read_data_into_dataframe('CSV', contingut)
        self.assertIsNotNone(df)
        self.assertEqual(len(df), 1)
        self.assertEqual(df.iloc[0]['nom'], 'camera1')

    def test_read_json_content(self):
        """Comprova la lectura de format JSON."""
        contingut = b'[{"id": 1, "nom": "camera_json"}]'
        df = read_data_into_dataframe('JSON', contingut)
        self.assertIsNotNone(df)
        self.assertEqual(df.iloc[0]['nom'], 'camera_json')

    def test_read_zip_with_csv(self):
        """Simula un fitxer ZIP que conté un CSV i verifica que s'extreu rectament."""
        # Creem un ZIP en memòria
        buf = BytesIO()
        with zipfile.ZipFile(buf, 'w') as z:
            z.writestr("cameres.csv", "id,camera\n100,camera_zip")

        df = read_data_into_dataframe('ZIP', buf.getvalue())
        self.assertIsNotNone(df)
        self.assertEqual(df.iloc[0]['camera'], 'camera_zip')

    ### --- TESTS DE LÒGICA DE NETEJA DE COLUMNES ---
    def test_column_cleaning_cameres(self):
        """Verifica la neteja de noms de columnes específica per a Càmeres."""
        # Simulem noms de columnes que podrien venir de JSON o ZIP
        df = pd.DataFrame(columns=["ID.Cam", "Nom (Càmera)", "Coord. X"])

        # Repliquem la lògica del main()
        cleaned_columns = [str(c).strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        expected = ["idcam", "nom_càmera", "coord_x"]
        self.assertListEqual(cleaned_columns, expected)

    ### --- TESTS DE CONFIGURACIÓ ---
    @patch("os.path.exists")
    def test_load_properties_empty(self, mock_exists):
        mock_exists.return_value = False
        res = load_properties("fake.path", {"existia": "si"})
        self.assertEqual(res["existia"], "si")

if __name__ == "__main__":
    unittest.main()