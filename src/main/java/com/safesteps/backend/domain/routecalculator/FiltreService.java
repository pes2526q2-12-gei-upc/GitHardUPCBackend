package com.safesteps.backend.domain.routecalculator;

import com.safesteps.backend.domain.common.exception.BadRequestException;
import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import com.safesteps.backend.domain.routecalculator.projections.FiltreDBProjection;
import org.springframework.stereotype.Service;
import org.slf4j.LoggerFactory;

@Service
public class FiltreService {
    private final FiltreRepository filtreRepository;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FiltreService.class);


    public FiltreService(FiltreRepository filtreRepository) {
        this.filtreRepository = filtreRepository;
    }

    public Filtre getFiltre(String googleId, FiltreEnum f) {
        Filtre filtre;
        if (f == FiltreEnum.PERSONALITZAT) {
            if (googleId == null) {
                throw new BadRequestException("Per als filtres personalitzats el googleId no pot ser null.");
            }
            FiltreDBProjection fPj = filtreRepository.findByGoogleId(googleId);
            if (fPj == null) {
                logger.warn("Filtre no trobat per googleId={} filtre={}", googleId, f);
                throw new ResourceNotFoundException("Usuari no existeix.");
            }
            filtre = new Filtre(fPj);
        } else {
            filtre = new Filtre(f);
            filtre.setGoogleId(googleId);
        }
        return filtre;
    }

}
