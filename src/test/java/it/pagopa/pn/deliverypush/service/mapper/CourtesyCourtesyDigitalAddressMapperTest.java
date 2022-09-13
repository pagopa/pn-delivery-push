package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyChannelType;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CourtesyCourtesyDigitalAddressMapperTest {

    @Test
    void externalToInternal() {

        CourtesyDigitalAddressInt actual = CourtesyCourtesyDigitalAddressMapper.externalToInternal(buildCourtesyDigitalAddress());

        Assertions.assertEquals(buildCourtesyDigitalAddressInt(), actual);
    }

    @Test
    void internalToExternal() {

        CourtesyDigitalAddress actual = CourtesyCourtesyDigitalAddressMapper.internalToExternal(buildCourtesyDigitalAddressInt());

        Assertions.assertEquals(buildCourtesyDigitalAddressInt().getAddress(), actual.getValue());
    }

    private CourtesyDigitalAddress buildCourtesyDigitalAddress() {
        CourtesyDigitalAddress address = new CourtesyDigitalAddress();
        address.setValue("001");
        address.setChannelType(CourtesyChannelType.EMAIL);
        return address;
    }

    private CourtesyDigitalAddressInt buildCourtesyDigitalAddressInt() {
        return CourtesyDigitalAddressInt.builder()
                .address("001")
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build();
    }
}