package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicRegistryCallDetailsIntTest {

    private PublicRegistryCallDetailsInt callDetailsInt;

    @BeforeEach
    void setUp() {
        callDetailsInt = new PublicRegistryCallDetailsInt();
        callDetailsInt.setRecIndex(1);
    }

    @Test
    void toLog() {
        Assertions.assertEquals("recIndex=1", callDetailsInt.toLog());
    }
}