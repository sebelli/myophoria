package myophoria.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UnitTest {
    @Test
    void convertsBetweenUnits() {
        assertEquals(45.359237, Unit.LB.convert(100, Unit.KG), 1e-9);
        assertEquals(100, Unit.KG.convert(45.359237, Unit.LB), 1e-9);
        assertEquals(70, Unit.KG.convert(70, Unit.KG), 1e-12);
    }

    @Test
    void workingSetTypes() {
        assertEquals(false, SetType.WARMUP.isWorking());
        assertEquals(true, SetType.WORKING.isWorking());
        assertEquals(true, SetType.FAILURE.isWorking());
    }
}
