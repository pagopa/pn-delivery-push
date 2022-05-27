package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.CourtesyDigitalAddressInt;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;

public class CourtesyDigitalAddressMapper {
    private CourtesyDigitalAddressMapper (){}
    
    public static CourtesyDigitalAddressInt externalToInternal(CourtesyDigitalAddress courtesyDigitalAddress) {
        return CourtesyDigitalAddressInt.builder()
                .address(courtesyDigitalAddress.getValue())
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE.valueOf(courtesyDigitalAddress.getChannelType().getValue()))
                .build();
    }

    public static CourtesyDigitalAddress internalToExternal(CourtesyDigitalAddressInt digitalAddress) {
        CourtesyDigitalAddress courtesyDigitalAddress = new CourtesyDigitalAddress();
        courtesyDigitalAddress.setValue(digitalAddress.getAddress());
        return courtesyDigitalAddress;
    }

}
