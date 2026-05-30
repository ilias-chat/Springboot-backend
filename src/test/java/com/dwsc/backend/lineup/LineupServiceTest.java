package com.dwsc.backend.lineup;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineupServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void summarizeStats_buildsGoalsAssistsApps() throws Exception {
        var stats =
                objectMapper.readTree(
                        "{\"goals\":{\"total\":12,\"assists\":5},\"games\":{\"appearences\":30}}");
        String summary = LineupService.summarizeStats(stats);
        assertTrue(summary.contains("goals:12"));
        assertTrue(summary.contains("assists:5"));
        assertTrue(summary.contains("apps:30"));
    }

    @Test
    void summarizeStats_returnsNullForEmpty() throws Exception {
        assertNull(LineupService.summarizeStats(objectMapper.readTree("{}")));
        assertNull(LineupService.summarizeStats(null));
    }

    @Test
    void validFormations_matchNodeBackend() {
        assertEquals(7, LineupService.VALID_FORMATIONS.size());
        assertTrue(LineupService.VALID_FORMATIONS.contains("4-2-3-1"));
    }
}
