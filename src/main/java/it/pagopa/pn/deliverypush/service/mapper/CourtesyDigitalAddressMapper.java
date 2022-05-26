package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;

public class CourtesyDigitalAddressMapper {
    private CourtesyDigitalAddressMapper (){}
    
    public static DigitalAddress externalToInternal(CourtesyDigitalAddress courtesyDigitalAddress) {
        return DigitalAddress.builder()
                .address(courtesyDigitalAddress.getValue())
                .build();
    }

    public static CourtesyDigitalAddress internalToExternal(DigitalAddress digitalAddress) {
        CourtesyDigitalAddress courtesyDigitalAddress = new CourtesyDigitalAddress();
        courtesyDigitalAddress.setValue(digitalAddress.getAddress());
        return courtesyDigitalAddress;
    }
}
