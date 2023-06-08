package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AarGenerationDetailsTest {

    private AarGenerationDetails details;

    @BeforeEach
    void setUp() {

        details = new AarGenerationDetails();
        details.setGeneratedAarUrl("http://localhost");
        details.setNumberOfPages(1);
        details.setRecIndex(100);
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(100, details.getRecIndex());
    }

    @Test
    void generatedAarUrl() {
        Assertions.assertEquals("http://localhost", details.getGeneratedAarUrl());
    }


    @Test
    void getNumberOfPages() {
        Assertions.assertEquals(1, details.getNumberOfPages());
    }

    @Test
    void testEquals() {
        AarGenerationDetails data = AarGenerationDetails.builder()
                .recIndex(100)
                .numberOfPages(1)
                .generatedAarUrl("http://localhost")
                .build();

        Assertions.assertEquals(Boolean.TRUE, details.equals(data));
    }
}