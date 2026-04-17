# per executar el test has de fer cd src/scripts i després python -m tests.test_DataLoad_Qualitat_aire
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import pandas as pd
from io import StringIO

from DataLoad_Qualitat_aire import load_properties, get_latest_csv_url

class TestDataLoadQualitatAire(unittest.TestCase):

    ### --- TESTS DE CONFIGURACIÓ ---
    @patch("os.path.exists")
    def test_load_properties_qualitat_aire(self, mock_exists):
        """Verifica que es carreguen les propietats de la taula de qualitat d'aire."""
        mock_exists.return_value = True
        content = "db.table.qualitat_aire=qualitat_aire_table\nopendata.qualitat_aire.url=https://api.qualitat"
        with patch("builtins.open", mock_open(read_data=content)):
            config = load_properties("fake.properties", {})
            self.assertEqual(config["db.table.qualitat_aire"], "qualitat_aire_table")
            self.assertEqual(config["opendata.qualitat_aire.url"], "https://api.qualitat")

    @patch("os.path.exists")
    def test_load_properties_file_not_found(self, mock_exists):
        """Verifica que si el fitxer no existeix, el diccionari no canvia."""
        mock_exists.return_value = False
        config = {"clau": "valor_original"}
        result = load_properties("no_existeix.properties", config)
        self.assertEqual(result["clau"], "valor_original")

    def test_load_properties_with_env_vars(self):
        """Verifica que les variables tipus ${VAR} es resolen des de l'entorn."""
        content = "spring.datasource.password=${SAFESTEPS_DB_PASSWORD}"
        with patch("builtins.open", mock_open(read_data=content)):
            with patch("os.path.exists", return_value=True):
                with patch.dict(os.environ, {"SAFESTEPS_DB_PASSWORD": "safe_testing_password_123"}):
                    config = {}
                    result = load_properties("fake.properties", config)
                    self.assertEqual(result["spring.datasource.password"], "safe_testing_password_123")

    def test_load_properties_ignores_comments(self):
        """Verifica que les línies comentades amb # s'ignoren."""
        content = "db.table.qualitat_aire=taula\n#opendata.qualitat_aire.url=https://ignorada\n"
        with patch("builtins.open", mock_open(read_data=content)):
            with patch("os.path.exists", return_value=True):
                config = {}
                result = load_properties("fake.properties", config)
                self.assertIn("db.table.qualitat_aire", result)
                self.assertNotIn("#opendata.qualitat_aire.url", result)

    ### --- TESTS D'API ---
    @patch("requests.get")
    def test_get_latest_csv_url_success(self, mock_get):
        """Simula una resposta correcta de l'API d'Open Data i verifica que es troba el CSV."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [
                    {"format": "PDF", "url": "https://opendata.bcn/qualitat.pdf"},
                    {"format": "CSV", "url": "https://opendata.bcn/qualitat.csv"}
                ]
            }
        }
        mock_get.return_value = mock_response

        url = get_latest_csv_url("https://api.test")
        self.assertEqual(url, "https://opendata.bcn/qualitat.csv")

    @patch("requests.get")
    def test_get_latest_csv_url_no_csv_resource(self, mock_get):
        """Verifica que retorna None si no hi ha cap recurs en format CSV."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [
                    {"format": "PDF", "url": "https://opendata.bcn/qualitat.pdf"},
                    {"format": "XML", "url": "https://opendata.bcn/qualitat.xml"}
                ]
            }
        }
        mock_get.return_value = mock_response

        url = get_latest_csv_url("https://api.test")
        self.assertIsNone(url)

    @patch("requests.get")
    def test_get_latest_csv_url_api_error(self, mock_get):
        """Verifica que si l'API falla, el script no peta i retorna None."""
        mock_get.side_effect = Exception("API Down")
        url = get_latest_csv_url("https://api.test")
        self.assertIsNone(url)

    @patch("requests.get")
    def test_get_latest_csv_url_success_false(self, mock_get):
        """Verifica que si l'API retorna success=False, es retorna None."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {"success": False}
        mock_get.return_value = mock_response

        url = get_latest_csv_url("https://api.test")
        self.assertIsNone(url)

    ### --- TESTS DE LÒGICA DE DADES (PANDAS) ---
    def test_dataframe_cleaning_qualitat_aire(self):
        """Verifica la neteja de columnes típica de dades de qualitat d'aire."""
        csv_content = "ID.Estació,Nom (Estació),Valor.NO2,Unitat (µg/m3)\n1,Eixample,45,µg/m3"
        df = pd.read_csv(StringIO(csv_content))

        df.columns = [c.strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        expected = ["idestació", "nom_estació", "valorno2", "unitat_µg/m3"]
        self.assertListEqual(list(df.columns), expected)

    def test_empty_dataframe_protection(self):
        """Comprova que el script detectaria si el CSV no té columnes útils."""
        csv_content = "Dades corruptes sense comes"
        df = pd.read_csv(StringIO(csv_content))

        is_invalid = df.shape[1] <= 1
        self.assertTrue(is_invalid)

    def test_valid_dataframe_structure(self):
        """Verifica que un CSV ben format supera la validació d'estructura."""
        csv_content = "estacio,contaminant,valor\n1,NO2,45"
        df = pd.read_csv(StringIO(csv_content))

        is_valid = df.shape[1] > 1
        self.assertTrue(is_valid)
        self.assertEqual(len(df), 1)

    ### --- TESTS DE FORMAT D'ARXIU ---
    def test_utf16_detection_qualitat_aire(self):
        """Verifica que el sistema de detecció d'encoding UTF-16 funciona correctament."""
        text_data = "estacio,contaminant,valor\n1,NO2,45"
        contingut_binari = text_data.encode('utf-16')

        if b'\x00' in contingut_binari:
            contingut_decodificat = contingut_binari.decode('utf-16')
        else:
            contingut_decodificat = contingut_binari.decode('utf-8')

        self.assertEqual(contingut_decodificat, text_data)

    def test_utf8_detection_qualitat_aire(self):
        """Verifica que el contingut UTF-8 estàndard es processa sense fer decode UTF-16."""
        text_data = "estacio,contaminant,valor\n1,O3,30"
        contingut_binari = text_data.encode('utf-8')

        # No ha de tenir bytes nuls, per tant ha d'anar pel camí UTF-8
        self.assertNotIn(b'\x00', contingut_binari)

        contingut_decodificat = contingut_binari.decode('utf-8')
        self.assertEqual(contingut_decodificat, text_data)

    ### --- TESTS DE SEGURETAT DE PARÀMETRES ---
    def test_params_validation_all_present(self):
        """Verifica que la lògica de validació no detecta errors si tots els paràmetres estan."""
        params = {
            "url_api": "https://api.test",
            "t_qualitat_aire": "qualitat_aire",
            "db_user": "user",
            "db_pass": "safe_testing_password_123",
            "db_url_jdbc": "jdbc:postgresql://localhost:5432/safesteps"
        }
        missing = [k for k, v in params.items() if not v]
        self.assertEqual(len(missing), 0)

    def test_params_validation_missing_url(self):
        """Verifica la detecció de paràmetres faltants quan falta la URL de l'API."""
        params = {
            "url_api": None,
            "t_qualitat_aire": "qualitat_aire",
            "db_user": "user",
            "db_pass": "safe_testing_password_123",
            "db_url_jdbc": "jdbc:postgresql://localhost:5432/safesteps"
        }
        missing = [k for k, v in params.items() if not v]
        self.assertEqual(len(missing), 1)
        self.assertIn("url_api", missing)

if __name__ == "__main__":
    unittest.main()