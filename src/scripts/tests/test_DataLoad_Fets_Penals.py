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
    def test_data_processing_logic(self):
        """Verifica la neteja, el filtratge i l'agrupació de dades de delictes."""

        # 1. Creem dades falses per posar a prova l'algorisme
        data = {
            'any': [2023, 2023, 2022, 2018, 2023], # El 2018 és massa antic, s'ha de descartar
            'area_basica_policial_abp': ['ABP Eixample', 'ABP Ciutat Vella', 'ABP Eixample', 'ABP Eixample', 'ABP Sabadell'], # Sabadell s'ha de descartar
            'tipus_de_fet': ['Robatori amb força', 'Lesions', 'Furt d\'ús de vehicle', 'Homicidi', 'Lesions'], # El Furt no té paraules clau perilloses, s'ha de descartar
            'coneguts': [10, 5, 20, 1, 50]
        }
        df = pd.DataFrame(data)

        # 2. Apliquem la mateixa lògica que fa el DataLoad_Fets_Penals.py
        max_year = df['any'].max()
        df = df[df['any'] >= (max_year - 2)]

        bcn_abps = ['ABP Sant Martí', 'ABP Ciutat Vella', 'ABP Eixample', 'ABP Sants-Montjuïc',
                    'ABP Les Corts', 'ABP Sarrià-Sant Gervasi', 'ABP Gràcia', 'ABP Horta-Guinardó',
                    'ABP Nou Barris', 'ABP Sant Andreu']
        df = df[df['area_basica_policial_abp'].isin(bcn_abps)]

        paraules_clau_perill = 'violència|lesions|sexual|homicidi|amenaces|coaccions|robatori amb força|estrebada'
        df = df[df['tipus_de_fet'].str.contains(paraules_clau_perill, case=False, na=False)]

        df_agrupat = df.groupby('area_basica_policial_abp')['coneguts'].sum().reset_index()
        df_agrupat['area_basica_policial_abp'] = df_agrupat['area_basica_policial_abp'].str.replace('ABP ', '', regex=False)
        df_agrupat = df_agrupat.rename(columns={
            'area_basica_policial_abp': 'nom_districte',
            'coneguts': 'total_delictes_vianants'
        })

        # 3. VERIFICACIONS (ASSERTS)

        # S'haurien d'haver descartat registres fins a quedar només amb Eixample i Ciutat Vella
        self.assertEqual(len(df_agrupat), 2)

        # L'Eixample només hauria de tenir 10 delictes (el de 2018 i el furt es descarten)
        eixample_crimes = df_agrupat[df_agrupat['nom_districte'] == 'Eixample']['total_delictes_vianants'].iloc[0]
        self.assertEqual(eixample_crimes, 10)

        # Ciutat Vella n'ha de tenir 5
        cv_crimes = df_agrupat[df_agrupat['nom_districte'] == 'Ciutat Vella']['total_delictes_vianants'].iloc[0]
        self.assertEqual(cv_crimes, 5)

        # Validem que les columnes finals es diuen exactament com volem
        expected_columns = ['nom_districte', 'total_delictes_vianants']
        self.assertListEqual(list(df_agrupat.columns), expected_columns)

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
            "db_user": "user",
            "db_pass": "", # Falta la contrasenya
            "db_url_jdbc": "jdbc:..."
        }
        missing = [k for k, v in params.items() if not v]
        self.assertIn("db_pass", missing)
        self.assertEqual(len(missing), 1)

if __name__ == '__main__':
    unittest.main()