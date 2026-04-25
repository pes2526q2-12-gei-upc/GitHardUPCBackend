package com.safesteps.backend;

import com.safesteps.backend.domain.routecalculator.Filtre;
import com.safesteps.backend.domain.routecalculator.FiltreEnum;
import com.safesteps.backend.domain.routecalculator.projections.FiltreDBProjection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FiltreTest {

    @Test
    void constructorSeguretat() {
        Filtre filtre = new Filtre(FiltreEnum.SEGURETAT);

        assertEquals(1.0, filtre.getComissaries());
        assertEquals(1.0, filtre.getFetsPenals());
        assertEquals(1.0, filtre.getCameresSeguretat());
        assertEquals(1.0, filtre.getInfraccions());
        assertEquals(0.0, filtre.getFontsAigua());
        assertEquals(0.0, filtre.getBancs());
        assertEquals(0.0, filtre.getContaminacioAcustica());
        assertEquals(0.0, filtre.getEscalesMecaniques());
        assertEquals(0.0, filtre.getArbres());
        assertEquals(0.0, filtre.getRefugisClimatics());
        assertEquals(0.0, filtre.getQualitatAire());

    }

    @Test
    void constructorClima() {
        Filtre filtre = new Filtre(FiltreEnum.CLIMA);
        assertEquals(0.0, filtre.getComissaries());
        assertEquals(0.0, filtre.getFetsPenals());
        assertEquals(0.0, filtre.getCameresSeguretat());
        assertEquals(0.0, filtre.getInfraccions());
        assertEquals(0.0, filtre.getFontsAigua());
        assertEquals(0.0, filtre.getBancs());
        assertEquals(0.0, filtre.getContaminacioAcustica());
        assertEquals(0.0, filtre.getEscalesMecaniques());
        assertEquals(1.0, filtre.getArbres());
        assertEquals(1.0, filtre.getRefugisClimatics());
        assertEquals(1.0, filtre.getQualitatAire());

    }

    @Test
    void constructorConfort() {
        Filtre filtre = new Filtre(FiltreEnum.CONFORT);
        assertEquals(0.0, filtre.getComissaries());
        assertEquals(0.0, filtre.getFetsPenals());
        assertEquals(0.0, filtre.getCameresSeguretat());
        assertEquals(0.0, filtre.getInfraccions());
        assertEquals(1.0, filtre.getFontsAigua());
        assertEquals(1.0, filtre.getBancs());
        assertEquals(1.0, filtre.getContaminacioAcustica());
        assertEquals(1.0, filtre.getEscalesMecaniques());
        assertEquals(0.0, filtre.getArbres());
        assertEquals(0.0, filtre.getRefugisClimatics());
        assertEquals(0.0, filtre.getQualitatAire());
    }



    @Test
    void constructorPersonalitzat() {
        FiltreDBProjection projection = new FiltreDBProjection() {
            @Override public Double getComissaries() { return null; }
            @Override public Double getFetsPenals() { return 0.2; }
            @Override public Double getCameresSeguretat() { return null; }
            @Override public Double getInfraccions() { return 0.4; }
            @Override public Double getFontsAigua() { return null; }
            @Override public Double getBancs() { return 0.6; }
            @Override public Double getContaminacioAcustica() { return 0.7; }
            @Override public Double getEscalesMecaniques() { return null; }
            @Override public Double getArbres() { return 0.9; }
            @Override public Double getRefugisClimatics() { return null; }
            @Override public Double getQualitatAire() { return 1.0; }
        };

        Filtre filtre = new Filtre(projection);

        assertEquals(0.5, filtre.getComissaries());
        assertEquals(0.2, filtre.getFetsPenals());
        assertEquals(0.5, filtre.getCameresSeguretat());
        assertEquals(0.4, filtre.getInfraccions());
        assertEquals(0.5, filtre.getFontsAigua());
        assertEquals(0.6, filtre.getBancs());
        assertEquals(0.7, filtre.getContaminacioAcustica());
        assertEquals(0.5, filtre.getEscalesMecaniques());
        assertEquals(0.9, filtre.getArbres());
        assertEquals(0.5, filtre.getRefugisClimatics());
        assertEquals(1.0, filtre.getQualitatAire());
    }


    @Test
    void constructorPersonalitzat2() {
        FiltreDBProjection projection = new FiltreDBProjection() {
            @Override public Double getComissaries() { return null; }
            @Override public Double getFetsPenals() { return null; }
            @Override public Double getCameresSeguretat() { return null; }
            @Override public Double getInfraccions() { return null; }
            @Override public Double getFontsAigua() { return null; }
            @Override public Double getBancs() { return null; }
            @Override public Double getContaminacioAcustica() { return null; }
            @Override public Double getEscalesMecaniques() { return null; }
            @Override public Double getArbres() { return null; }
            @Override public Double getRefugisClimatics() { return null; }
            @Override public Double getQualitatAire() { return null; }
        };

        Filtre filtre = new Filtre(projection);

        assertEquals(0.5, filtre.getComissaries());
        assertEquals(0.5, filtre.getFetsPenals());
        assertEquals(0.5, filtre.getCameresSeguretat());
        assertEquals(0.5, filtre.getInfraccions());
        assertEquals(0.5, filtre.getFontsAigua());
        assertEquals(0.5, filtre.getBancs());
        assertEquals(0.5, filtre.getContaminacioAcustica());
        assertEquals(0.5, filtre.getEscalesMecaniques());
        assertEquals(0.5, filtre.getArbres());
        assertEquals(0.5, filtre.getRefugisClimatics());
        assertEquals(0.5, filtre.getQualitatAire());
    }


    @Test
    void constructorPersonalitzat3() {
        FiltreDBProjection projection = new FiltreDBProjection() {
            @Override public Double getComissaries() { return 1.0; }
            @Override public Double getFetsPenals() { return 1.0; }
            @Override public Double getCameresSeguretat() { return 1.0; }
            @Override public Double getInfraccions() { return 1.0; }
            @Override public Double getFontsAigua() { return 1.0; }
            @Override public Double getBancs() { return 1.0; }
            @Override public Double getContaminacioAcustica() { return 1.0; }
            @Override public Double getEscalesMecaniques() { return 1.0; }
            @Override public Double getArbres() { return 1.0; }
            @Override public Double getRefugisClimatics() { return 1.0; }
            @Override public Double getQualitatAire() { return 1.0; }
        };

        Filtre filtre = new Filtre(projection);

        assertEquals(1.0, filtre.getComissaries());
        assertEquals(1.0, filtre.getFetsPenals());
        assertEquals(1.0, filtre.getCameresSeguretat());
        assertEquals(1.0, filtre.getInfraccions());
        assertEquals(1.0, filtre.getFontsAigua());
        assertEquals(1.0, filtre.getBancs());
        assertEquals(1.0, filtre.getContaminacioAcustica());
        assertEquals(1.0, filtre.getEscalesMecaniques());
        assertEquals(1.0, filtre.getArbres());
        assertEquals(1.0, filtre.getRefugisClimatics());
        assertEquals(1.0, filtre.getQualitatAire());
    }

    @Test
    void constructorOutOfFilterEnum() {
        Filtre filtre = new Filtre((FiltreEnum) null);

        assertEquals(0.0, filtre.getComissaries());
        assertEquals(0.0, filtre.getFetsPenals());
        assertEquals(0.0, filtre.getCameresSeguretat());
        assertEquals(0.0, filtre.getInfraccions());
        assertEquals(0.0, filtre.getFontsAigua());
        assertEquals(0.0, filtre.getBancs());
        assertEquals(0.0, filtre.getContaminacioAcustica());
        assertEquals(0.0, filtre.getEscalesMecaniques());
        assertEquals(0.0, filtre.getArbres());
        assertEquals(0.0, filtre.getRefugisClimatics());
        assertEquals(0.0, filtre.getQualitatAire());
    }

    @Test
    void constructorOutOfFilterProj() {
        Filtre filtre = new Filtre((FiltreDBProjection) null);
        assertEquals(0.0, filtre.getComissaries());
        assertEquals(0.0, filtre.getFetsPenals());
        assertEquals(0.0, filtre.getCameresSeguretat());
        assertEquals(0.0, filtre.getInfraccions());
        assertEquals(0.0, filtre.getFontsAigua());
        assertEquals(0.0, filtre.getBancs());
        assertEquals(0.0, filtre.getContaminacioAcustica());
        assertEquals(0.0, filtre.getEscalesMecaniques());
        assertEquals(0.0, filtre.getArbres());
        assertEquals(0.0, filtre.getRefugisClimatics());
        assertEquals(0.0, filtre.getQualitatAire());
    }
}

