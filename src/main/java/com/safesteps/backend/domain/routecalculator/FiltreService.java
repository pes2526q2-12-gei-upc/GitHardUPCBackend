package com.safesteps.backend.domain.routecalculator;

import com.safesteps.backend.domain.routecalculator.projections.FiltreDBProjection;
import org.springframework.stereotype.Service;

@Service
public class FiltreService {
    private final FiltreRepository filtreRepository;

    public FiltreService(FiltreRepository filtreRepository) {
        this.filtreRepository = filtreRepository;
    }

    public Filtre getFiltre(String googleId, FiltreEnum f) {
        Filtre filtre;
        if (f == FiltreEnum.PERSONALITZAT) {
            if (googleId == null) return null;
            FiltreDBProjection fPj = filtreRepository.findByGoogleId(googleId);
            if (fPj == null) return null;
            filtre = new Filtre(fPj);
        } else {
            filtre = new Filtre(f);
            filtre.setGoogleId(googleId);
        }
        return filtre;
    }

}
