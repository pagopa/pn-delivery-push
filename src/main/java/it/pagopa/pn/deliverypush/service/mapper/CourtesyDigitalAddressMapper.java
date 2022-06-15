package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;

public class CourtesyDigitalAddressMapper {
    private CourtesyDigitalAddressMapper(){}
    
    public static CourtesyDigitalAddressInt digitalToCourtesy(DigitalAddress courtesyDigitalAddress) {
        if (courtesyDigitalAddress == null)
            return  null;
        return CourtesyDigitalAddressInt.builder()
                .address(courtesyDigitalAddress.getAddress())
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE.valueOf(courtesyDigitalAddress.getType()))
                .build();
    }

    public static DigitalAddress courtesyToDigital(CourtesyDigitalAddressInt digitalAddress) {
        DigitalAddress courtesyDigitalAddress = new DigitalAddress();
        courtesyDigitalAddress.setAddress(digitalAddress.getAddress());
        courtesyDigitalAddress.setType(digitalAddress.getType().getValue());
        return courtesyDigitalAddress;
    }

}
