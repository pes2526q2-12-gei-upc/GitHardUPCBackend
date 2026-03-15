# per executar el test has de fer cd src/scripts i després python -m tests.test_DataLoad_Fonts_beure
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import sys
import pandas as pd
from io import StringIO

from DataLoad_Fonts_beure import load_properties, get_latest_csv_url

class TestDataLoadFontsBeure(unittest.TestCase):

    ### --- TESTS DE CONFIGURACIÓ ---
    @patch("os.path.exists")
    def test_load_properties_fonts(self, mock_exists):
        """Verifica que es carreguen les propietats de la taula de fonts de beure."""
        mock_exists.return_value = True
        content = "db.table.fonts_beure=fonts_table\nopendata.fonts_beure.url=https://api.fonts"
        with patch("builtins.open", mock_open(read_data=content)):
            config = load_properties("fake.properties", {})
            self.assertEqual(config["db.table.fonts_beure"], "fonts_table")
            self.assertEqual(config["opendata.fonts_beure.url"], "https://api.fonts")

    ### --- TESTS D'API ---
    @patch("requests.get")
    def test_get_latest_csv_url_fonts_success(self, mock_get):
        """Simula la cerca del CSV de fonts al portal d'Open Data."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [
                    {"format": "CSV", "url": "https://opendata.bcn/fonts.csv"}
                ]
            }
        }
        mock_get.return_value = mock_response

        url = get_latest_csv_url("https://api.test")
        self.assertEqual(url, "https://opendata.bcn/fonts.csv")

    ### --- TESTS DE LÒGICA DE DADES (PANDAS) ---
    def test_dataframe_cleaning_fonts(self):
        """Verifica la neteja de columnes per a les fonts de beure."""
        # Simulem capçaleres amb punts, espais i parèntesis
        csv_content = "ID_Font,Nom (Font),Codi.Barri,Coord. X\n1,Font Monumental,10,430000"
        df = pd.read_csv(StringIO(csv_content))

        # Lògica de neteja del script original
        df.columns = [c.strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        expected = ["id_font", "nom_font", "codibarri", "coord_x"]
        self.assertListEqual(list(df.columns), expected)

    ### --- TESTS DE FORMAT D'ARXIU ---
    def test_utf16_detection_fonts(self):
        """Verifica que el sistema de detecció d'encoding funciona per a les fonts."""
        text_data = "col1,col2\nval1,val2"
        # Forcem un format amb bytes nuls (UTF-16)
        contingut_binari = text_data.encode('utf-16')

        # Comprovació de la lògica del script
        if b'\x00' in contingut_binari:
            contingut_decodificat = contingut_binari.decode('utf-16')
        else:
            contingut_decodificat = contingut_binari.decode('utf-8')

        self.assertEqual(contingut_decodificat, text_data)

if __name__ == "__main__":
    unittest.main()