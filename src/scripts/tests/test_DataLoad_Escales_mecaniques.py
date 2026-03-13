# per executar el test has de fer cd src/scripts i després python -m tests.test_DataLoad_Escales_mecaniques
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import sys
import pandas as pd
from io import StringIO

from DataLoad_Escales_mecaniques import load_properties, get_latest_csv_url

class TestDataLoadEscalesMecaniques(unittest.TestCase):

    ### --- TESTS DE CONFIGURACIÓ ---
    @patch("os.path.exists")
    def test_load_properties_escales(self, mock_exists):
        """Verifica la càrrega de propietats específica per a escales mecaniques."""
        mock_exists.return_value = True
        content = "db.table.escales_mecaniques=stair_table\nopendata.escales_mecaniques.url=https://api.stairs"
        with patch("builtins.open", mock_open(read_data=content)):
            config = load_properties("fake.properties", {})
            self.assertEqual(config["db.table.escales_mecaniques"], "stair_table")
            self.assertEqual(config["opendata.escales_mecaniques.url"], "https://api.stairs")

    ### --- TESTS D'API ---
    @patch("requests.get")
    def test_get_latest_csv_url_found(self, mock_get):
        """Verifica que es troba el recurs CSV correctament."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [
                    {"format": "CSV", "url": "https://opendata.bcn/escales.csv"},
                    {"format": "KML", "url": "https://opendata.bcn/escales.kml"}
                ]
            }
        }
        mock_get.return_value = mock_response

        url = get_latest_csv_url("https://api.test")
        self.assertEqual(url, "https://opendata.bcn/escales.csv")

    ### --- TESTS DE LÒGICA DE DADES (PANDAS) ---
    def test_dataframe_cleaning_escales(self):
        """Verifica la neteja de columnes per a les escales mecàniques."""
        # Simulem dades amb noms complexos (espais, parèntesis, accents)
        csv_content = "ID_Escala,Localització (Carrer),Estat.Operatiu\n1,Escala de Prova,Activa"
        df = pd.read_csv(StringIO(csv_content))

        # Repliquem la lògica de neteja del script original
        df.columns = [c.strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        expected = ["id_escala", "localització_carrer", "estatoperatiu"]
        self.assertListEqual(list(df.columns), expected)

    ### --- TESTS DE ROBUSTESA (ERROR HANDLING) ---
    @patch("requests.get")
    def test_get_latest_csv_url_error(self, mock_get):
        """Verifica que si l'API falla, el script no peta i retorna None."""
        mock_get.side_effect = Exception("API Down")
        url = get_latest_csv_url("https://api.test")
        self.assertIsNone(url)

if __name__ == "__main__":
    unittest.main()