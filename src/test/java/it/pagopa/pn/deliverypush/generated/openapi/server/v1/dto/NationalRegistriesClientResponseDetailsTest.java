package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NationalRegistriesClientResponseDetailsTest {

    private PublicRegistryResponseDetails details;

    @BeforeEach
    void setUp() {
        details = new PublicRegistryResponseDetails();
        details.setRecIndex(1);
        details.setDigitalAddress(DigitalAddress.builder().address("add").build());
        details.setPhysicalAddress(PhysicalAddress.builder().address("add").build());
    }

    @Test
    void recIndex() {
        PublicRegistryResponseDetails tmp = PublicRegistryResponseDetails.builder()
                .recIndex(1)
                .digitalAddress(DigitalAddress.builder().address("add").build())
                .physicalAddress(PhysicalAddress.builder().address("add").build())
                .build();
        Assertions.assertEquals(tmp, details.recIndex(1));
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, details.getRecIndex());
    }

    @Test
    void digitalAddress() {
        PublicRegistryResponseDetails tmp = PublicRegistryResponseDetails.builder()
                .recIndex(1)
                .digitalAddress(DigitalAddress.builder().address("add").build())
                .physicalAddress(PhysicalAddress.builder().address("add").build())
                .build();
        Assertions.assertEquals(tmp, details.digitalAddress(DigitalAddress.builder().address("add").build()));
    }

    @Test
    void getDigitalAddress() {
        Assertions.assertEquals(DigitalAddress.builder().address("add").build(), details.getDigitalAddress());
    }

    @Test
    void physicalAddress() {
        PublicRegistryResponseDetails tmp = PublicRegistryResponseDetails.builder()
                .recIndex(1)
                .digitalAddress(DigitalAddress.builder().address("add").build())
                .physicalAddress(PhysicalAddress.builder().address("add").build())
                .build();
        Assertions.assertEquals(tmp, details.physicalAddress(PhysicalAddress.builder().address("add").build()));
    }

    @Test
    void getPhysicalAddress() {
        Assertions.assertEquals(PhysicalAddress.builder().address("add").build(), details.getPhysicalAddress());
    }

    @Test
    void testEquals() {
        PublicRegistryResponseDetails tmp = PublicRegistryResponseDetails.builder()
                .recIndex(1)
                .digitalAddress(DigitalAddress.builder().address("add").build())
                .physicalAddress(PhysicalAddress.builder().address("add").build())
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }
}