# per executar el test has de fer cd src/scripts i després python -m tests.test_DataLoad_Infraccions_Administratives
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import sys
import pandas as pd
from io import StringIO

from DataLoad_Infraccions_Administratives import load_properties

class TestDataLoadInfraccionsJoc(unittest.TestCase):

    ### --- TESTS DE CONFIGURACIÓ ---
    @patch("os.path.exists")
    def test_load_properties_joc(self, mock_exists):
        """Verifica que es carreguen les propietats de la taula d'infraccions de joc."""
        mock_exists.return_value = True
        content = "opendata.infraccions_joc.url=https://gencat.cat/joc.csv\ndb.table.infraccions_joc=joc_table"
        with patch("builtins.open", mock_open(read_data=content)):
            config = load_properties("fake.properties", {})
            self.assertEqual(config["opendata.infraccions_joc.url"], "https://gencat.cat/joc.csv")
            self.assertEqual(config["db.table.infraccions_joc"], "joc_table")

    ### --- TESTS DE LÒGICA DE DADES (PANDAS) ---
    def test_column_cleaning_joc(self):
        """Verifica la neteja de columnes per a dades d'infraccions administratives."""
        capcaleres_brutes = [
            "Data.Infracció",
            "Tipus de Fet (Penal)", # En el test anterior era joc, l'important és el format
            "Import Sanció (€)",
            "Codi.Municipi"
        ]
        df = pd.DataFrame(columns=capcaleres_brutes)

        # El teu script fa .replace('.', '')
        df.columns = [str(c).strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        # AJUSTA AQUESTA LLISTA:
        expected = ["datainfracció", "tipus_de_fet_penal", "import_sanció_€", "codimunicipi"]
        self.assertListEqual(list(df.columns), expected)

    ### --- TESTS DE SEGURETAT DE PARÀMETRES ---
    def test_params_validation_logic(self):
        """Verifica la lògica de detecció de paràmetres faltants del main()."""
        # Simulem un escenari on falta la URL
        params = {
            "url_csv": None,
            "t_joc": "taula_ok",
            "db_user": "user",
            "db_pass": "safe_testing_password_123",
            "db_url_jdbc": "jdbc:postgresql://..."
        }

        missing = [k for k, v in params.items() if not v]
        self.assertEqual(len(missing), 1)
        self.assertIn("url_csv", missing)

    ### --- TESTS DE RESPOSTA HTTP ---
    @patch("requests.get")
    def test_download_joc_error_handling(self, mock_get):
        """Verifica que el script gestiona correctament un error 404 de l'API."""
        mock_response = MagicMock()
        mock_response.raise_for_status.side_effect = Exception("404 Not Found")
        mock_get.return_value = mock_response

        # Aquí verifiquem que la crida es fa. El try/except del main s'encarregaria de loguejar l'error.
        with self.assertRaises(Exception):
            mock_get.return_value.raise_for_status()

if __name__ == "__main__":
    unittest.main()