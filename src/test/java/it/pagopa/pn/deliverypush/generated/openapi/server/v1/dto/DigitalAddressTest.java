package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DigitalAddressTest {

    private DigitalAddress address;

    @BeforeEach
    void setUp() {
        address = new DigitalAddress();
        address.setAddress("address 001");
        address.setType("type");
    }

    @Test
    void getType() {
        Assertions.assertEquals("type", address.getType());
    }

    @Test
    void getAddress() {
        Assertions.assertEquals("address 001", address.getAddress());
    }

    @Test
    void testEquals() {
        DigitalAddress data = DigitalAddress.builder()
                .address("address 001")
                .type("type")
                .build();
        Assertions.assertEquals(Boolean.TRUE, address.equals(data));
    }

    @Test
    void testToString() {
        String data = "class DigitalAddress {\n" +
                "    type: type\n" +
                "    address: address 001\n" +
                "}";
        Assertions.assertEquals(data, address.toString());
    }

    @Test
    void testAddress() {
        DigitalAddress data = address.address("address 001");
        Assertions.assertEquals(data, address.address("address 001"));
    }

    @Test
    void testType() {
        DigitalAddress data = address.type("type");
        Assertions.assertEquals(data, address.type("type"));
    }

}