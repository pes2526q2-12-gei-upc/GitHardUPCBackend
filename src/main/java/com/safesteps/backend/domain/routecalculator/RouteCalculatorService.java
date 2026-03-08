package com.safesteps.backend.domain.routecalculator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteCalculatorService {

    @Autowired
    private CarrerRepository carrerRepository;

    public List<Long> getBestRoute(Double originLat, Double originLong, Double endLat, Double endLong) {
        //Com ens arriba en lat i long, ho hem de passar als identificadors dels carrers mes propers
        Long start = carrerRepository.findNearestNode(originLat, originLong);
        Long end = carrerRepository.findNearestNode(endLat, endLong);
        //Un cop els tinguem, podem trobar el cami mes curt
        return carrerRepository.findShortestPath(start, end);
    }
}