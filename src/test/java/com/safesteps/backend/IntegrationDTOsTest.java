package com.safesteps.backend;

import com.safesteps.backend.domain.routecalculator.EventExternalResponseDTO;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationDTOsTest {

    @Test
    void testCategoryDTO() {
        EventExternalResponseDTO.CategoryDTO category = new EventExternalResponseDTO.CategoryDTO();
        category.setName("Rutes");

        assertEquals("Rutes", category.getName());
    }

    @Test
    void testEventDTO() {
        EventExternalResponseDTO.CategoryDTO cat = new EventExternalResponseDTO.CategoryDTO();
        cat.setName("Concerts");

        EventExternalResponseDTO.EventDTO event = new EventExternalResponseDTO.EventDTO();
        event.setDenomination("Festival");
        event.setLatitude(41.38);
        event.setLongitude(2.17);
        event.setFree(true);
        event.setCategories(Collections.singletonList(cat));

        assertEquals("Festival", event.getDenomination());
        assertEquals(41.38, event.getLatitude());
        assertEquals(2.17, event.getLongitude());
        assertTrue(event.getFree());
        assertEquals(1, event.getCategories().size());
        assertEquals("Concerts", event.getCategories().get(0).getName());
    }

    @Test
    void testEventExternalResponseDTO() {
        EventExternalResponseDTO.EventDTO event = new EventExternalResponseDTO.EventDTO();
        event.setDenomination("Test");

        EventExternalResponseDTO response = new EventExternalResponseDTO();
        response.setResults(Collections.singletonList(event));

        assertNotNull(response.getResults());
        assertEquals(1, response.getResults().size());
        assertEquals("Test", response.getResults().get(0).getDenomination());
    }
}