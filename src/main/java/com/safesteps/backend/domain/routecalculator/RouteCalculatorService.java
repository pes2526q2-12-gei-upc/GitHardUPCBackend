package com.safesteps.backend.domain.routecalculator;

import com.safesteps.backend.domain.routecalculator.projections.CoordDBProjection;
import com.safesteps.backend.domain.routecalculator.projections.RouteDBProjection;
import com.safesteps.backend.domain.routecalculator.projections.PoiDBProjection;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RouteCalculatorService {

    //Injeccio per constructor (Clean Architecture)
    private final CarrerRepository carrerRepository;
    private final EventIntegrationService eventIntegrationService;

    public RouteCalculatorService(CarrerRepository carrerRepository, EventIntegrationService eventIntegrationService) {
        this.carrerRepository = carrerRepository;
        this.eventIntegrationService = eventIntegrationService;
    }

    public RouteResponseDTO getBestRoute(Coord org, Coord dest, int nRoutes, Filtre filtre) {
        // La validación automática con @InsideBarcelona se encarga de verificar que las coordenadas estén dentro de Barcelona

        // Com ens arriba en lat i long, ho hem de passar als identificadors dels carrers mes propers
        Long start = carrerRepository.findNearestNode(org.getLat(), org.getLon());
        Long end = carrerRepository.findNearestNode(dest.getLat(), dest.getLon());

        // Conjunt de nodes que s'han visitat en la ruta principal o en les alternatives. Ens permet crear rutes alt. que intentin no passar per aquests nodes i així generar rutes diferents.
        Set<Long> edgesVisitats = new HashSet<>();
        RouteResponseDTO response = new RouteResponseDTO();
        for (int i = 0; i < nRoutes; ++i)
            response.getRoutes().add(calculRuta(edgesVisitats,  start, end, filtre));

        return response;
    }

    private Route calculRuta(Set<Long> edgesVisitats, Long org, Long dest, Filtre filtre) {
        String formattedEdges = formatEdgesSetForDB(edgesVisitats);
        List<RouteDBProjection> rutaPrincipal = carrerRepository.findPathWithPenalties(org, dest, formattedEdges, filtre);

        Long[] rutaPrincipalFID = new Long[rutaPrincipal.size()];
        double distanceMeters = 0.0;
        int i = 0;

        // Extraurem els IDs dels nodes i sumem la distància física real pas a pas
        for (RouteDBProjection rp : rutaPrincipal) {
            if (rp.getEdge() != null && rp.getEdge() > 0) {
                edgesVisitats.add(rp.getEdge());
                rutaPrincipalFID[i++] = rp.getNode();
                distanceMeters += rp.getCost();
            }

        }

        // Obtenim les coordenades per pintar la línia de la ruta
        List<CoordDBProjection> rutaFIDCoords = carrerRepository.getCoordsFromNodeIds(rutaPrincipalFID);
        List<Double[]> ruta = rutaFIDCoords.stream()
                .map(r -> new Double[]{ r.getLon(), r.getLat() })
                .toList();

        List<PoiDTO> pois = fetchRoutePois(rutaPrincipalFID, filtre);
        List<PoiDTO> esdevenimentsPois = fetchExternalEvents(rutaFIDCoords);
        pois.addAll(esdevenimentsPois);

        // 3. Retornem la Ruta enriquida (Ara el constructor de Route demana els POIs al final)
        return new Route(
                ruta,
                Math.round(distanceMeters * 100.0) / 100.0,
                calculateEstimatedTime(distanceMeters),
                pois
        );
    }

    private List<PoiDTO> fetchRoutePois(Long[] rutaPrincipalFID, Filtre filtre) {
        List<PoiDTO> pois = new ArrayList<>();

        if (isFiltreSeguretat(filtre)) {
            addPois(pois, carrerRepository.findComissariesNearRoute(rutaPrincipalFID, 500.0), "COMISSARIA");
            addPois(pois, carrerRepository.findCameresNearRoute(rutaPrincipalFID, 100.0), "CAMERA");
        } else if (isFiltreConfort(filtre)) {
            addPois(pois, carrerRepository.findBancsNearRoute(rutaPrincipalFID, 30.0), "BANC");
            addPois(pois, carrerRepository.findFontsNearRoute(rutaPrincipalFID, 75.0), "FONT");
            addPois(pois, carrerRepository.findEscalesNearRoute(rutaPrincipalFID, 50.0), "ESCALA_MECANICA");
        } else if (isFiltreClima(filtre)) {
            addPois(pois, carrerRepository.findFontsNearRoute(rutaPrincipalFID, 75.0), "FONT");
            addPois(pois, carrerRepository.findRefugisNearRoute(rutaPrincipalFID, 100.0), "REFUGI_CLIMATIC");
        } else {
            addPersonalizedPois(pois, rutaPrincipalFID, filtre);
        }

        addPois(pois, carrerRepository.findIncidentsNearRoute(rutaPrincipalFID, 50.0), "INCIDENCIA");

        return pois;
    }


    private List<PoiDTO> fetchExternalEvents(List<CoordDBProjection> rutaFIDCoords) {
        // Transformem els resultats de la BD als objectes Coord
        List<Coord> routeCoords = rutaFIDCoords.stream()
                .map(r -> {
                    Coord c = new Coord();
                    c.setLat(r.getLat());
                    c.setLon(r.getLon());
                    return c;
                })
                .toList();

        // Cridem la API del grup extern
        return eventIntegrationService.getEventsForRoute(routeCoords);
    }


    private boolean isFiltreSeguretat(Filtre filtre) {
        return filtre.getComissaries() == 1 && filtre.getFetsPenals() == 1 && filtre.getCameresSeguretat() == 1 && filtre.getInfraccions() == 1 && filtre.getFontsAigua() == 0;
    }

    private boolean isFiltreConfort(Filtre filtre) {
        return filtre.getFontsAigua() == 1 && filtre.getBancs() == 1 && filtre.getContaminacioAcustica() == 1 && filtre.getEscalesMecaniques() == 1 && filtre.getComissaries() == 0;
    }

    private boolean isFiltreClima(Filtre filtre) {
        return filtre.getArbres() == 1 && filtre.getRefugisClimatics() == 1 && filtre.getQualitatAire() == 1 && filtre.getComissaries() == 0;
    }

    private void addPersonalizedPois(List<PoiDTO> pois, Long[] rutaPrincipalFID, Filtre filtre) {
        double avg = (filtre.getComissaries() + filtre.getFetsPenals() + filtre.getCameresSeguretat() + filtre.getInfraccions() +
                filtre.getFontsAigua() + filtre.getBancs() + filtre.getContaminacioAcustica() + filtre.getEscalesMecaniques() +
                filtre.getArbres() + filtre.getRefugisClimatics() + filtre.getQualitatAire()) / 11.0;

        if (filtre.getComissaries() >= avg) addPois(pois, carrerRepository.findComissariesNearRoute(rutaPrincipalFID, 500.0), "COMISSARIA");
        if (filtre.getFontsAigua() >= avg) addPois(pois, carrerRepository.findFontsNearRoute(rutaPrincipalFID, 75.0), "FONT");
        if (filtre.getBancs() >= avg) addPois(pois, carrerRepository.findBancsNearRoute(rutaPrincipalFID, 30.0), "BANC");
        if (filtre.getCameresSeguretat() >= avg) addPois(pois, carrerRepository.findCameresNearRoute(rutaPrincipalFID, 100.0), "CAMERA");
        if (filtre.getEscalesMecaniques() >= avg) addPois(pois, carrerRepository.findEscalesNearRoute(rutaPrincipalFID, 50.0), "ESCALA_MECANICA");
        if (filtre.getRefugisClimatics() >= avg) addPois(pois, carrerRepository.findRefugisNearRoute(rutaPrincipalFID, 100.0), "REFUGI_CLIMATIC");
    }

    private void addPois(List<PoiDTO> pois, List<PoiDBProjection> dbProjections, String type) {
        pois.addAll(dbProjections.stream()
                .map(p -> new PoiDTO(type, p.getName(), p.getLat(), p.getLon()))
                .toList());
    }

    // Calcular el temps estimat (Velocitat a peu de 5 km/h -> ~83.33 m/min)
    private int calculateEstimatedTime(double distance) {
        int time = (int) Math.round(distance / 75);
        return (time == 0 && distance > 0) ? 1  : time;
    }

    private String formatEdgesSetForDB(Set<Long> edgesVisitats) {
        return edgesVisitats.isEmpty() ? "-1" : String.join(",",
                edgesVisitats.stream().map(String::valueOf).toArray(String[]::new));
    }
}