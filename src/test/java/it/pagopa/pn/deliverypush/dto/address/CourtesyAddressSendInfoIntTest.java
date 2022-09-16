package it.pagopa.pn.deliverypush.dto.address;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

class CourtesyAddressSendInfoIntTest {

    private CourtesyAddressSendInfoInt courtesyAddressSendInfoInt;

    @BeforeEach
    public void setup() {
        courtesyAddressSendInfoInt = new CourtesyAddressSendInfoInt();
        courtesyAddressSendInfoInt.setSent(Boolean.TRUE);
        courtesyAddressSendInfoInt.setCourtesyDigitalAddress(CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL).build());
    }

    @Test
    void getCourtesyDigitalAddress() {
        Assertions.assertEquals(CourtesyDigitalAddressInt.builder()
                        .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL).build(),
                courtesyAddressSendInfoInt.getCourtesyDigitalAddress());
    }

    @Test
    void setCourtesyDigitalAddress() throws NoSuchFieldException, IllegalAccessException {
        CourtesyAddressSendInfoInt pojo = new CourtesyAddressSendInfoInt();

        pojo.setCourtesyDigitalAddress(CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL).build());

        final Field field = pojo.getClass().getDeclaredField("courtesyDigitalAddress");
        field.setAccessible(true);
        Assertions.assertEquals(CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL).build(), field.get(pojo));
    }

    @Test
    void getSent() {

        Assertions.assertEquals(Boolean.TRUE, courtesyAddressSendInfoInt.getSent());
    }

    @Test
    void setSent() throws NoSuchFieldException, IllegalAccessException {
        CourtesyAddressSendInfoInt pojo = new CourtesyAddressSendInfoInt();

        pojo.setSent(Boolean.TRUE);

        final Field field = pojo.getClass().getDeclaredField("sent");
        field.setAccessible(true);
        Assertions.assertEquals(Boolean.TRUE, field.get(pojo));
    }

}