package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;

public class LegalDigitalAddressMapper {

    private LegalDigitalAddressMapper(){}

    public static LegalDigitalAddressInt digitalToLegal(DigitalAddress legalDigitalAddress) {
        if (legalDigitalAddress == null)
            return  null;
        return LegalDigitalAddressInt.builder()
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.valueOf(legalDigitalAddress.getType()))
                .address(legalDigitalAddress.getAddress())
                .build();
    }

    public static DigitalAddress legalToDigital(LegalDigitalAddressInt digitalAddress) {
        if (digitalAddress == null)
            return  null;
        return DigitalAddress.builder()
                .address(digitalAddress.getAddress())
                .type(digitalAddress.getType().getValue())
                .build();
    }
}
