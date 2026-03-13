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

    public RouteCalculatorService(CarrerRepository carrerRepository) {
        this.carrerRepository = carrerRepository;
    }

    public RouteResponseDTO getBestRoute(Coord org, Coord dest, int nRoutes) {
        // Com ens arriba en lat i long, ho hem de passar als identificadors dels carrers mes propers
        Long start = carrerRepository.findNearestNode(org.getLat(), org.getLon());
        Long end = carrerRepository.findNearestNode(dest.getLat(), dest.getLon());

        // Conjunt de nodes que s'han visitat en la ruta principal o en les alternatives. Ens permet crear rutes alt. que intentin no passar per aquests nodes i així generar rutes diferents.
        Set<Long> edgesVisitats = new HashSet<>();
        RouteResponseDTO response = new RouteResponseDTO();
        for (int i = 0; i < nRoutes; ++i)
            response.getRoutes().add(calculRuta(edgesVisitats,  start, end));

        return response;
    }

    private Route calculRuta(Set<Long> edgesVisitats, Long org, Long dest) {
        String formattedEdges = formatEdgesSetForDB(edgesVisitats);
        List<RouteDBProjection> rutaPrincipal = carrerRepository.findPathWithPenalties(org, dest, formattedEdges);

        Long[] rutaPrincipalFID = new Long[rutaPrincipal.size()];
        double distanceMeters = 0.0;
        int i = 0;

        // Extraurem els IDs dels nodes i sumem la distància física real pas a pas
        for (RouteDBProjection rp : rutaPrincipal) {
            if (rp.getEdge() != null && rp.getEdge() > 0) edgesVisitats.add(rp.getEdge());
            rutaPrincipalFID[i++] = rp.getNode();
            distanceMeters += rp.getCost();
        }

        // Obtenim les coordenades per pintar la línia de la ruta
        List<CoordDBProjection> rutaFIDCoords = carrerRepository.getCoordsFromNodeIds(rutaPrincipalFID);
        List<Double[]> ruta = rutaFIDCoords.stream()
                .map(r -> new Double[]{ r.getLon(), r.getLat() })
                .toList();

        List<PoiDTO> pois = new ArrayList<>();

        // A. Comissaries (Radi ample: 500 metres, útil per a seguretat a la zona)
        List<PoiDBProjection> comissariesDB = carrerRepository.findComissariesNearRoute(rutaPrincipalFID, 500.0);
        pois.addAll(comissariesDB.stream()
                .map(c -> new PoiDTO("COMISSARIA", c.getName(), c.getLat(), c.getLon()))
                .toList());

        // B. Fonts de beure (Radi curt: 75 metres, l'usuari no vol desviar-se molt per beure aigua)
        List<PoiDBProjection> fontsDB = carrerRepository.findFontsNearRoute(rutaPrincipalFID, 100.0);
        pois.addAll(fontsDB.stream()
                .map(f -> new PoiDTO("FONT", f.getName(), f.getLat(), f.getLon()))
                .toList());

        // C. Bancs (Radi molt curt: 30 metres. N'hi ha milers, només volem els que estan literalment al camí)
        List<PoiDBProjection> bancsDB = carrerRepository.findBancsNearRoute(rutaPrincipalFID, 30.0);
        pois.addAll(bancsDB.stream()
                .map(b -> new PoiDTO("BANC", b.getName(), b.getLat(), b.getLon()))
                .toList());

        // D. Càmeres de seguretat (Radi: 100 metres. Cobreixen un cert camp de visió)
        List<PoiDBProjection> cameresDB = carrerRepository.findCameresNearRoute(rutaPrincipalFID, 100.0);
        pois.addAll(cameresDB.stream()
                .map(c -> new PoiDTO("CAMERA", c.getName(), c.getLat(), c.getLon()))
                .toList());

        // E. Escales Mecàniques (Radi: 50 metres. Molt útils per evitar desnivells pronunciats)
        List<PoiDBProjection> escalesDB = carrerRepository.findEscalesNearRoute(rutaPrincipalFID, 50.0);
        pois.addAll(escalesDB.stream()
                .map(e -> new PoiDTO("ESCALA_MECANICA", e.getName(), e.getLat(), e.getLon()))
                .toList());

        // F. Arbrat Viari (Radi: 15 metres. Vital per a l'ombra a l'estiu, però limitem el radi per evitar sobrecàrrega de dades)
        List<PoiDBProjection> arbresDB = carrerRepository.findArbresNearRoute(rutaPrincipalFID, 15.0);
        pois.addAll(arbresDB.stream()
                .map(a -> new PoiDTO("ARBRE", a.getName(), a.getLat(), a.getLon()))
                .toList());

        // G. Arbrat de Zona (Radi: 15 metres. Arbres dins de parcs o places per on passa la ruta)
        List<PoiDBProjection> arbresZonaDB = carrerRepository.findArbresZonaNearRoute(rutaPrincipalFID, 15.0);
        pois.addAll(arbresZonaDB.stream()
                .map(a -> new PoiDTO("ARBRE", a.getName(), a.getLat(), a.getLon()))
                .toList());


        // 3. Retornem la Ruta enriquida (Ara el constructor de Route demana els POIs al final)
        return new Route(
                ruta,
                Math.round(distanceMeters * 100.0) / 100.0,
                calculateEstimatedTime(distanceMeters),
                pois
        );
    }

    // Calcular el temps estimat (Velocitat a peu de 5 km/h -> ~83.33 m/min)
    private int calculateEstimatedTime(double distance) {
        int time = (int) Math.round(distance / 83.33);
        return (time == 0 && distance > 0) ? 1  : time;
    }

    private String formatEdgesSetForDB(Set<Long> edgesVisitats) {
        return edgesVisitats.isEmpty() ? "-1" : String.join(",",
                edgesVisitats.stream().map(String::valueOf).toArray(String[]::new));
    }
}