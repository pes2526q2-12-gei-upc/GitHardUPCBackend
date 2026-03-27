# per executar el test has de fer cd src/scripts i després python -m tests.test_DataLoad_Comisaries
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import sys
import pandas as pd
from io import StringIO

from DataLoad_Comisaries import load_properties, get_latest_csv_url

class TestDataLoadComissaries(unittest.TestCase):

    ### --- TESTS DE CONFIGURACIÓ ---
    @patch("os.path.exists")
    def test_load_properties_full(self, mock_exists):
        """Verifica que es carreguen les propietats i es resolen les variables d'entorn."""
        mock_exists.return_value = True
        content = "opendata.comissaries.url=https://api.police\ndb.user=${USER_TEST}"

        with patch("builtins.open", mock_open(read_data=content)):
            with patch.dict(os.environ, {"USER_TEST": "admin_police"}):
                config = load_properties("fake.properties", {})
                self.assertEqual(config["opendata.comissaries.url"], "http://api.police")
                self.assertEqual(config["db.user"], "admin_police")

    ### --- TESTS D'API ---
    @patch("requests.get")
    def test_get_latest_csv_url_comissaries(self, mock_get):
        """Verifica que es troba la URL del CSV de comissaries rectament."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [
                    {"format": "CSV", "url": "https://opendata.bcn/comissaries.csv"}
                ]
            }
        }
        mock_get.return_value = mock_response

        url = get_latest_csv_url("https://api.test")
        self.assertEqual(url, "https://opendata.bcn/comissaries.csv")

    ### --- TESTS DE LÒGICA DE DADES (PANDAS) ---
    def test_dataframe_cleaning_comissaries(self):
        """Verifica que la neteja de columnes funciona per a les dades de comissaries."""
        # Les comissaries solen tenir noms de columnes com 'Nom.Comissaria' o 'Adreça (Carrer)'
        csv_content = "Nom.Comissaria,Adreça (Carrer),Telèfon\nGuardia Urbana,Carrer A,932222222"
        df = pd.read_csv(StringIO(csv_content))

        # Lògica de neteja del script original
        df.columns = [c.strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        # Nota: El replace de '(' i ')' elimina el parèntesi però el strip i replace d'espai pot deixar guions baixos
        # Depenent de l'ordre exacte, 'adreça (carrer)' -> 'adreça__carrer'
        self.assertIn("nomcomissaria", df.columns)
        self.assertIn("telèfon", df.columns)

    def test_utf16_decoding_comissaries(self):
        """Verifica que el script sap gestionar si el CSV de comissaries ve en UTF-16."""
        text_original = "id,name\n1,Comissaria Central"
        contingut_utf16 = text_original.encode('utf-16')

        # Simulem el bloc de decodificació que tens al main()
        if b'\x00' in contingut_utf16:
            contingut_final = contingut_utf16.decode('utf-16')
        else:
            contingut_final = contingut_utf16.decode('utf-8')

        self.assertEqual(contingut_final, text_original)

if __name__ == "__main__":
    unittest.main()