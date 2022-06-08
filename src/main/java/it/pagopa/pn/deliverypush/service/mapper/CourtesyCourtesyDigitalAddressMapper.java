package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.CourtesyDigitalAddressInt;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyChannelType;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;

public class CourtesyCourtesyDigitalAddressMapper {
    private CourtesyCourtesyDigitalAddressMapper(){}
    
    public static CourtesyDigitalAddressInt externalToInternal(CourtesyDigitalAddress courtesyDigitalAddress) {
        return CourtesyDigitalAddressInt.builder()
                .address(courtesyDigitalAddress.getValue())
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE.valueOf(courtesyDigitalAddress.getChannelType().getValue()))
                .build();
    }

    public static CourtesyDigitalAddress internalToExternal(CourtesyDigitalAddressInt digitalAddress) {
        CourtesyDigitalAddress courtesyDigitalAddress = new CourtesyDigitalAddress();
        courtesyDigitalAddress.setValue(digitalAddress.getAddress());
        courtesyDigitalAddress.setChannelType(CourtesyChannelType.valueOf(digitalAddress.getType().getValue()));
        return courtesyDigitalAddress;
    }

}
