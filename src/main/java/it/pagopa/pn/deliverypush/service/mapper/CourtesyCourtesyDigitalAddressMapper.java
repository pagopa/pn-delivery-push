package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.CourtesyChannelType;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.CourtesyDigitalAddress;

public class CourtesyCourtesyDigitalAddressMapper {
    private CourtesyCourtesyDigitalAddressMapper(){}
    
    public static CourtesyDigitalAddressInt externalToInternal(CourtesyDigitalAddress courtesyDigitalAddress) {
        return CourtesyDigitalAddressInt.builder()
                .address(courtesyDigitalAddress.getValue())
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.valueOf(courtesyDigitalAddress.getChannelType().getValue()))
                .build();
    }

    public static CourtesyDigitalAddress internalToExternal(CourtesyDigitalAddressInt digitalAddress) {
        CourtesyDigitalAddress courtesyDigitalAddress = new CourtesyDigitalAddress();
        courtesyDigitalAddress.setValue(digitalAddress.getAddress());
        courtesyDigitalAddress.setChannelType(CourtesyChannelType.valueOf(digitalAddress.getType().getValue()));
        return courtesyDigitalAddress;
    }

}
