# per executar el test has de fer cd src/scripts i després python -m tests.test_DataLoad_Fets_Penals
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import sys
import pandas as pd
from io import StringIO

from DataLoad_Fets_Penals import load_properties

class TestDataLoadFetsPenals(unittest.TestCase):

    ### --- TESTS DE CONFIGURACIÓ ---
    @patch("os.path.exists")
    def test_load_properties_fets_penals(self, mock_exists):
        """Verifica que es carreguen les configuracions de Gencat i DB."""
        mock_exists.return_value = True
        content = "opendata.fets_penals.url=https://gencat.cat/fets.csv\ndb.table.fets_penals=fets_table"
        with patch("builtins.open", mock_open(read_data=content)):
            config = load_properties("fake.properties", {})
            self.assertEqual(config["opendata.fets_penals.url"], "https://gencat.cat/fets.csv")
            self.assertEqual(config["db.table.fets_penals"], "fets_table")

    ### --- TESTS DE LÒGICA DE DADES (PANDAS) ---
    def test_column_cleaning_fets_penals(self):
        """Verifica la neteja de columnes típica de dades policials."""
        capcaleres_brutes = [
            "Any.Mes",
            "Tipus de Fet (Penal)",
            "Codi.Àrea_Bàsica",
            "Nombre d'Incidents"
        ]
        df = pd.DataFrame(columns=capcaleres_brutes)

        # Lògica del teu script: elimina punts, no els canvia per "_"
        df.columns = [str(c).strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        # MODIFICA AQUESTA LLISTA:
        expected = ["anymes", "tipus_de_fet_penal", "codiàrea_bàsica", "nombre_d'incidents"]
        self.assertListEqual(list(df.columns), expected)

    ### --- TESTS DE DESCÀRREGA (REQUESTS) ---
    @patch("requests.get")
    def test_download_fets_penals_success(self, mock_get):
        """Simula una descàrrega correcta del CSV de Gencat."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.text = "data,valor\n2023,100"
        mock_get.return_value = mock_response

        # Verifiquem que podem llegir el text simulat amb pandas
        df = pd.read_csv(StringIO(mock_get.return_value.text))
        self.assertEqual(len(df), 1)
        self.assertEqual(df.iloc[0]['valor'], 100)

    ### --- TESTS DE SEGURETAT DE PARÀMETRES ---
    def test_missing_config_logic(self):
        """Simula la lògica de comprovació de paràmetres buits del main()."""
        params = {
            "url_csv": "https://test",
            "t_fets": "taula",
            "db_user": "admin",
            "db_pass": "safe_testing_password_123",
            "db_url_jdbc": "jdbc:postgresql://localhost"
        }

        missing = [k for k, v in params.items() if not v]
        self.assertIn("db_pass", missing)
        self.assertEqual(len(missing), 1)

if __name__ == "__main__":
    unittest.main()