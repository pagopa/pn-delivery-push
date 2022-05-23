package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyChannelType;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;

public class CourtesyDigitalAddressMapper {
    
    public static DigitalAddress externalToInternal(CourtesyDigitalAddress courtesyDigitalAddress) {
        return DigitalAddress.builder()
                .type(DigitalAddress.TypeEnum.valueOf(courtesyDigitalAddress.getChannelType().getValue()))
                .address(courtesyDigitalAddress.getValue())
                .build();
    }

    public static CourtesyDigitalAddress internalToExternal(DigitalAddress digitalAddress) {
        CourtesyDigitalAddress courtesyDigitalAddress = new CourtesyDigitalAddress();
        courtesyDigitalAddress.setChannelType(CourtesyChannelType.valueOf(digitalAddress.getType().getValue()));
        courtesyDigitalAddress.setValue(digitalAddress.getAddress());

        return courtesyDigitalAddress;
    }
}
