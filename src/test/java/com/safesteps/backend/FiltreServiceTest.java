package com.safesteps.backend;

import com.safesteps.backend.domain.routecalculator.Filtre;
import com.safesteps.backend.domain.routecalculator.FiltreEnum;
import com.safesteps.backend.domain.routecalculator.FiltreRepository;
import com.safesteps.backend.domain.routecalculator.FiltreService;
import com.safesteps.backend.domain.routecalculator.projections.FiltreDBProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FiltreServiceTest {

    @Mock
    private FiltreRepository filtreRepository;

    @InjectMocks
    private FiltreService filtreService;

    @Test
    void getFiltreSeguretat() {
        Filtre filtre = filtreService.getFiltre("user-1", FiltreEnum.SEGURETAT);

        assertNotNull(filtre);
        assertEquals("user-1", filtre.getGoogleId());
        assertEquals(1.0, filtre.getComissaries());
        assertEquals(1.0, filtre.getFetsPenals());
        assertEquals(1.0, filtre.getCameresSeguretat());
        assertEquals(1.0, filtre.getInfraccions());
        assertEquals(0.0, filtre.getFontsAigua());
        verifyNoInteractions(filtreRepository);
    }

    @Test
    void getFiltreClima() {
        Filtre filtre = filtreService.getFiltre("user-2", FiltreEnum.CLIMA);

        assertNotNull(filtre);
        assertEquals(1.0, filtre.getArbres());
        assertEquals(1.0, filtre.getRefugisClimatics());
        assertEquals(1.0, filtre.getQualitatAire());
        assertEquals(0.0, filtre.getComissaries());
        verifyNoInteractions(filtreRepository);
    }

    @Test
    void getFiltreConfort() {
        Filtre filtre = filtreService.getFiltre("user-3", FiltreEnum.CONFORT);

        assertNotNull(filtre);
        assertEquals(1.0, filtre.getFontsAigua());
        assertEquals(1.0, filtre.getBancs());
        assertEquals(1.0, filtre.getContaminacioAcustica());
        assertEquals(1.0, filtre.getEscalesMecaniques());
        assertEquals(0.0, filtre.getComissaries());
        verifyNoInteractions(filtreRepository);
    }

    @Test
    void getFiltrePersonalitzatIdNull() {
        Filtre filtre = filtreService.getFiltre(null, FiltreEnum.PERSONALITZAT);
        assertNull(filtre);
    }

    @Test
    void getFiltrePersonalitzatFPJNull() {
        when(filtreRepository.findByGoogleId(any())).thenReturn(null);
        Filtre filtre = filtreService.getFiltre("a", FiltreEnum.PERSONALITZAT);
        assertNull(filtre);
    }

    @Test
    void getFiltrePersonalitzatId() {
        FiltreDBProjection projection = new FiltreDBProjection() {
            @Override public Double getComissaries() { return 0.1; }
            @Override public Double getFetsPenals() { return 0.2; }
            @Override public Double getCameresSeguretat() { return 0.3; }
            @Override public Double getInfraccions() { return 0.4; }
            @Override public Double getFontsAigua() { return 0.5; }
            @Override public Double getBancs() { return 0.6; }
            @Override public Double getContaminacioAcustica() { return 0.7; }
            @Override public Double getEscalesMecaniques() { return 0.8; }
            @Override public Double getArbres() { return 0.9; }
            @Override public Double getRefugisClimatics() { return 1.0; }
            @Override public Double getQualitatAire() { return null; }
        };
        when(filtreRepository.findByGoogleId("user-4")).thenReturn(projection);

        Filtre filtre = filtreService.getFiltre("user-4", FiltreEnum.PERSONALITZAT);

        assertNotNull(filtre);
        assertEquals(0.1, filtre.getComissaries());
        assertEquals(0.8, filtre.getEscalesMecaniques());
        assertEquals(0.5, filtre.getQualitatAire());
        verify(filtreRepository).findByGoogleId("user-4");
    }
}

