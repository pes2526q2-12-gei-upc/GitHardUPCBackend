package com.safesteps.backend.domain.routecalculator;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsideBarcelonaValidatorTest {

    @Mock
    private BarcelonaBoundaryService boundaryService;

    @Mock
    private ConstraintValidatorContext context;

    @InjectMocks
    private InsideBarcelonaValidator validator;

    @Test
    void isValid_NullCoord_ReturnsTrueToLetNotNullHandleIt() {
        // Si el objeto entero es nulo, dejamos que @NotNull haga su trabajo
        assertTrue(validator.isValid(null, context));
        verifyNoInteractions(boundaryService);
    }

    @Test
    void isValid_NullLatOrLon_ReturnsTrueToLetNotNullHandleIt() {
        Coord coordNoLat = new Coord();
        coordNoLat.setLon(2.17);
        assertTrue(validator.isValid(coordNoLat, context));

        Coord coordNoLon = new Coord();
        coordNoLon.setLat(41.38);
        assertTrue(validator.isValid(coordNoLon, context));

        verifyNoInteractions(boundaryService);
    }

    @Test
    void isValid_CoordInsideBarcelona_ReturnsTrue() {
        // Arrange
        Coord validCoord = new Coord();
        validCoord.setLat(41.3871);
        validCoord.setLon(2.1700);
        when(boundaryService.isWithinBarcelona(2.1700, 41.3871)).thenReturn(true);

        // Act
        boolean result = validator.isValid(validCoord, context);

        // Assert
        assertTrue(result);
        verify(boundaryService, times(1)).isWithinBarcelona(anyDouble(), anyDouble());
    }

    @Test
    void isValid_CoordOutsideBarcelona_ReturnsFalse() {
        // Arrange
        Coord invalidCoord = new Coord();
        invalidCoord.setLat(40.4168);
        invalidCoord.setLon(-3.7038);
        when(boundaryService.isWithinBarcelona(-3.7038, 40.4168)).thenReturn(false);

        // Act
        boolean result = validator.isValid(invalidCoord, context);

        // Assert
        assertFalse(result);
        verify(boundaryService, times(1)).isWithinBarcelona(anyDouble(), anyDouble());
    }
}