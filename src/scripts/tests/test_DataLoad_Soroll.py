# per executar el test has de fer cd src/scripts i després python -m tests.test_DataLoad_Soroll
import unittest
from unittest.mock import patch, MagicMock, mock_open
import os
import pandas as pd
import zipfile
from io import StringIO, BytesIO
from datetime import datetime

# Importem les funcions bàsiques de l'script principal
from DataLoad_Soroll import load_properties, get_latest_resource_info

class TestDataLoadSoroll(unittest.TestCase):

    ### --- TESTS DE CONFIGURACIÓ ---
    @patch("os.path.exists")
    def test_load_properties_soroll(self, mock_exists):
        """Verifica que es carreguen les propietats del fitxer properties."""
        mock_exists.return_value = True
        content = "db.table.soroll=bcn_contaminacio_acustica\nopendata.soroll.url=https://api.soroll"
        with patch("builtins.open", mock_open(read_data=content)):
            config = load_properties("fake.properties", {})
            self.assertEqual(config["db.table.soroll"], "bcn_contaminacio_acustica")
            self.assertEqual(config["opendata.soroll.url"], "https://api.soroll")

    ### --- TESTS D'API (SUPORT ZIP/CSV) ---
    @patch("requests.get")
    def test_get_latest_resource_info_zip(self, mock_get):
        """Verifica que troba el recurs si és un ZIP (com passa amb les lectures)."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "success": True,
            "result": {
                "resources": [{"format": "ZIP", "url": "https://opendata.bcn/soroll.zip"}]
            }
        }
        mock_get.return_value = mock_response
        url, fmt = get_latest_resource_info("https://api.test")
        self.assertEqual(url, "https://opendata.bcn/soroll.zip")
        self.assertEqual(fmt, "ZIP")

    ### --- TESTS DE LÒGICA DE DADES (PANDAS) ---
    def test_neteja_columnes(self):
        """Verifica la normalització de noms de columnes (Molt important per l'id_instal)."""
        df = pd.DataFrame(columns=["Id Instal", "Valor (dB)", "Data.Observacio"])
        df.columns = [str(c).strip().lower().replace(' ', '_').replace('.', '').replace('(', '').replace(')', '') for c in df.columns]

        expected = ["id_instal", "valor_db", "dataobservacio"]
        self.assertListEqual(list(df.columns), expected)

    def test_filtre_temps_logic(self):
        """Verifica la lògica del chunking: que només es guarden les dades d'aquest mes i any."""
        any_actual = datetime.now().year
        mes_actual = datetime.now().month

        # Simulem formats d'hora UTC
        data_vella = f"{any_actual - 1}-01-01T12:00:00Z"
        data_nova = f"{any_actual}-{mes_actual:02d}-15T12:00:00Z"

        df = pd.DataFrame({
            "timestamp_local": [data_vella, data_nova],
            "valor": [50, 60]
        })

        # Apliquem EXACTAMENT la lògica del script de producció
        df['data_temp'] = pd.to_datetime(df['timestamp_local'], errors='coerce', utc=True)
        df_filtrat = df[
            (df['data_temp'].dt.year == any_actual) &
            (df['data_temp'].dt.month == mes_actual)
            ]

        # Comprovació: L'antiga (50dB) desapareix, la nova (60dB) es queda.
        self.assertEqual(len(df_filtrat), 1)
        self.assertEqual(df_filtrat.iloc[0]['valor'], 60)

    def test_creuament_i_agregacio(self):
        """Verifica l'Inner Join per 'id_instal' i el càlcul de la mitjana de soroll per coordenada física."""
        # 1. Simulem df_lectures (Múltiples lectures per a un mateix sensor)
        df_lectures = pd.DataFrame({
            "id_instal": [101, 101, 102],
            "valor": [50.0, 60.0, 80.0]
        })

        # 2. Simulem df_inventari (Ubicació de cada sensor, incloent-hi un trencat)
        df_inventari = pd.DataFrame({
            "id_instal": [101, 102, 999], # El 999 no té lectures
            "latitud": [41.38, 41.39, 41.40],
            "longitud": [2.16, 2.17, 2.18]
        })

        # 3. Lògica del main()
        df_merged = pd.merge(df_lectures, df_inventari, on='id_instal', how='inner')
        df_final = df_merged.groupby(['latitud', 'longitud'])['valor'].mean().reset_index()
        df_final.columns = ['latitud', 'longitud', 'nivell_db']

        # Comprovacions
        self.assertEqual(len(df_final), 2, "El sensor 999 s'ha d'eliminar en el procés de Join")

        # Comprovem que el sensor 101 (Lat 41.38) ha fet bé la mitjana entre 50 i 60 (resultat = 55)
        valor_101 = df_final[df_final['latitud'] == 41.38]['nivell_db'].iloc[0]
        self.assertEqual(valor_101, 55.0, "El càlcul de la mitjana de decibels ha fallat")

if __name__ == "__main__":
    unittest.main()