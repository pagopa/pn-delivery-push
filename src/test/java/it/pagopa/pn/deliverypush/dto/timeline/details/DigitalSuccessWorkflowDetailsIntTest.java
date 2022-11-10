package it.pagopa.pn.deliverypush.dto.timeline.details;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
class DigitalSuccessWorkflowDetailsIntTest {
    private DigitalSuccessWorkflowDetailsInt detailsInt;
    @BeforeEach
    void setUp() {
        detailsInt = new DigitalSuccessWorkflowDetailsInt();
        detailsInt.setRecIndex(1);
        detailsInt.setDigitalAddress(LegalDigitalAddressInt.builder().address("address").build());
    }
    @Test
    void toLog() {
        String expected = "recIndex=1 digitalAddress='Sensitive information'";
        Assertions.assertEquals(expected, detailsInt.toLog());
    }
    @Test
    void testEquals() {
        DigitalSuccessWorkflowDetailsInt expected = buildDigitalSuccessWorkflowDetailsInt();
        Assertions.assertEquals(Boolean.TRUE, detailsInt.equals(expected));
    }
    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, detailsInt.getRecIndex());
    }
    @Test
    void getDigitalAddress() {
        Assertions.assertEquals(LegalDigitalAddressInt.builder().address("address").build(), detailsInt.getDigitalAddress());
    }
    @Test
    void testToString() {
        String expected = "DigitalSuccessWorkflowDetailsInt(recIndex=1, digitalAddress=LegalDigitalAddressInt(type=null))";
        Assertions.assertEquals(expected, detailsInt.toString());
    }
    private DigitalSuccessWorkflowDetailsInt buildDigitalSuccessWorkflowDetailsInt() {
        return DigitalSuccessWorkflowDetailsInt.builder().digitalAddress(LegalDigitalAddressInt.builder().address("address").build()).recIndex(1).build();
    }
}