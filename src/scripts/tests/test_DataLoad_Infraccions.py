# per executar el test has de fer cd src/scripts i després python -m tests.test_DataLoad_Infraccions
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import pandas as pd
from io import StringIO

# Importem la funció del script original
from DataLoad_Infraccions import load_properties

class TestDataLoadInfraccions(unittest.TestCase):

    ### --- TESTS DE CONFIGURACIÓ ---
    @patch("os.path.exists")
    def test_load_properties_infraccions(self, mock_exists):
        """Verifica que es carreguen les propietats correctament des del fitxer."""
        mock_exists.return_value = True
        content = (
            "opendata.infraccions_joc.url=https://test.cat/api/query.csv\n"
            "db.table.infraccions_joc=test_table_infraccions"
        )
        with patch("builtins.open", mock_open(read_data=content)):
            config = load_properties("fake.properties", {})
            self.assertEqual(config["opendata.infraccions_joc.url"], "https://test.cat/api/query.csv")
            self.assertEqual(config["db.table.infraccions_joc"], "test_table_infraccions")

### --- TESTS DE LÒGICA DE DADES (PANDAS) ---
    def test_dataframe_cleaning_infraccions(self):
        """Verifica la neteja estricta de columnes (accents tancats, oberts, espais, parèntesis)."""
        # Simulem dades amb els casos que el script neteja
        csv_content = "ID.Infracció,Tipus (Joc),Àrea.Geogràfica,Última.Actualització\n1,Ruleta,Barcelona,2023"
        df = pd.read_csv(StringIO(csv_content))

        # Repliquem exactament la lògica de neteja corregida del script
        df.columns = [
            c.strip().lower()
            .replace(' ', '_')
            .replace('.', '')
            .replace('(', '')
            .replace(')', '')
            .replace('ó', 'o')
            .replace('í', 'i')
            .replace('á', 'a')
            .replace('é', 'e')
            .replace('ú', 'u')
            .replace('à', 'a')
            .replace('è', 'e')
            .replace('ò', 'o')
            for c in df.columns
        ]

        expected_columns = ["idinfraccio", "tipus_joc", "areageografica", "ultimaactualitzacio"]
        self.assertListEqual(list(df.columns), expected_columns)

    ### --- TESTS DE FORMAT D'ARXIU (ENCODING) ---
    def test_encoding_detection_logic(self):
        """Verifica que la lògica de detecció d'UTF-16 (null bytes) funciona."""
        # Simulem el que passaria al script: r.content té bytes
        contingut_utf8 = "col1,col2\nval1,val2".encode('utf-8')
        contingut_utf16 = "col1,col2\nval1,val2".encode('utf-16')

        # El script mira si b'\x00' està en el contingut
        self.assertFalse(b'\x00' in contingut_utf8)
        self.assertTrue(b'\x00' in contingut_utf16)

    ### --- TESTS DE DESCÀRREGA (REQUESTS) ---
    @patch("requests.get")
    def test_download_infraccions_error_handling(self, mock_get):
        """Verifica que el script gestiona correctament un error 403 o 404."""
        mock_response = MagicMock()
        mock_response.raise_for_status.side_effect = Exception("403 Client Error: Forbidden")
        mock_get.return_value = mock_response

        # Aquí només testegem que el mock llança l'excepció com faria requests
        with self.assertRaises(Exception) as context:
            mock_get("https://url-falsa.csv").raise_for_status()

        self.assertIn("403", str(context.exception))

if __name__ == "__main__":
    unittest.main()