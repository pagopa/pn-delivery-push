package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimpleRegisteredLetterDetailsIntTest {

    private SimpleRegisteredLetterDetailsInt detailsInt;

    @BeforeEach
    public void setup() {
        detailsInt = SimpleRegisteredLetterDetailsInt.builder()
                .recIndex(0)
                .foreignState("001")
                .numberOfPages(1)
                .physicalAddress(PhysicalAddressInt.builder().addressDetails("000").build())
                .build();
    }

    @Test
    void toLog() {
        String log = detailsInt.toLog();
        Assertions.assertEquals("recIndex=0 physicalAddress='Sensitive information'", log);
    }

    @Test
    void getRecIndex() {
        int rec = detailsInt.getRecIndex();
        Assertions.assertEquals(0, rec);
    }

    @Test
    void getPhysicalAddress() {
        PhysicalAddressInt addressInt = detailsInt.getPhysicalAddress();
        Assertions.assertEquals("000", addressInt.getAddressDetails());
    }

    @Test
    void getForeignState() {
        String state = detailsInt.getForeignState();
        Assertions.assertEquals("001", state);
    }

    @Test
    void getNumberOfPages() {
        int pages = detailsInt.getNumberOfPages();
        Assertions.assertEquals(1, pages);
    }

    @Test
    void testToString() {
        String details = detailsInt.toString();
        Assertions.assertEquals("SimpleRegisteredLetterDetailsInt(recIndex=0, physicalAddress=PhysicalAddressInt(at=null, address=null, addressDetails=000, zip=null, municipality=null, municipalityDetails=null, province=null, foreignState=null), foreignState=001, numberOfPages=1)", details);
    }
}