package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DigitalSuccessWorkflowDetailsTest {

    private DigitalSuccessWorkflowDetails details;

    @BeforeEach
    void setUp() {
        DigitalAddress address = DigitalAddress.builder().address("aa").type("test").build();
        details = new DigitalSuccessWorkflowDetails();
        details.digitalAddress(address);
        details.setRecIndex(1);
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, details.getRecIndex());
    }

    @Test
    void digitalAddress() {
        DigitalAddress address = DigitalAddress.builder().address("aa").type("test").build();

        DigitalSuccessWorkflowDetails data = DigitalSuccessWorkflowDetails.builder()
                .recIndex(1)
                .digitalAddress(address)
                .build();

        Assertions.assertEquals(data, details);
    }

    @Test
    void getDigitalAddress() {
        DigitalAddress address = DigitalAddress.builder().address("aa").type("test").build();
        Assertions.assertEquals(address, details.getDigitalAddress());
    }

    @Test
    void testEquals() {
        DigitalAddress address = DigitalAddress.builder().address("aa").type("test").build();

        DigitalSuccessWorkflowDetails data = DigitalSuccessWorkflowDetails.builder()
                .recIndex(1)
                .digitalAddress(address)
                .build();
        Assertions.assertEquals(Boolean.TRUE, details.equals(data));
    }

    @Test
    void testToString() {
        String data = "class DigitalSuccessWorkflowDetails {\n" +
                "    recIndex: 1\n" +
                "    digitalAddress: class DigitalAddress {\n" +
                "        type: test\n" +
                "        address: aa\n" +
                "    }\n" +
                "}";
        Assertions.assertEquals(data, details.toString());
    }
}