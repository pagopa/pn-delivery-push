package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimpleRegisteredLetterDetailsTest {

    private SimpleRegisteredLetterDetails details;

    @BeforeEach
    void setUp() {
        details = new SimpleRegisteredLetterDetails();
        details.setPhysicalAddress(PhysicalAddress.builder().address("add").build());
        details.setRecIndex(1);
        details.setAnalogCost(1);
        details.productType("NR_AR");
    }

    @Test
    void recIndex() {
        SimpleRegisteredLetterDetails data = buildSimpleRegisteredLetterDetails();

        SimpleRegisteredLetterDetails actual = details.recIndex(1);

        Assertions.assertEquals(data, actual);
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, details.getRecIndex());
    }

    @Test
    void physicalAddress() {
        SimpleRegisteredLetterDetails data = buildSimpleRegisteredLetterDetails();

        SimpleRegisteredLetterDetails actual = details.physicalAddress(PhysicalAddress.builder().address("add").build());

        Assertions.assertEquals(data, actual);
    }

    @Test
    void getPhysicalAddress() {
        Assertions.assertEquals(PhysicalAddress.builder().address("add").build(), details.getPhysicalAddress());
    }

    @Test
    void analogCost() {
        SimpleRegisteredLetterDetails data = buildSimpleRegisteredLetterDetails();

        SimpleRegisteredLetterDetails actual = details.analogCost(1);

        Assertions.assertEquals(data, actual);
    }

    @Test
    void getAnalogCost() {

        Assertions.assertEquals(1, details.getAnalogCost());
    }

    @Test
    void testEquals() {
        SimpleRegisteredLetterDetails data = buildSimpleRegisteredLetterDetails();

        Assertions.assertEquals(Boolean.TRUE, details.equals(data));
    }

    @Test
    void testToString() {
        String expected = "class SimpleRegisteredLetterDetails {\n" +
                "    recIndex: 1\n" +
                "    physicalAddress: class PhysicalAddress {\n" +
                "        at: null\n" +
                "        address: add\n" +
                "        addressDetails: null\n" +
                "        zip: null\n" +
                "        municipality: null\n" +
                "        municipalityDetails: null\n" +
                "        province: null\n" +
                "        foreignState: null\n" +
                "    }\n" +
                "    productType: NR_AR\n" +
                "    analogCost: 1\n" +
                "}";

        Assertions.assertEquals(expected, details.toString());
    }

    private SimpleRegisteredLetterDetails buildSimpleRegisteredLetterDetails() {
        return SimpleRegisteredLetterDetails.builder()
                .recIndex(1)
                .analogCost(1)
                .productType("NR_AR")
                .physicalAddress(PhysicalAddress.builder().address("add").build())
                .build();
    }
}