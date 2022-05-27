package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;

public class LegalDigitalAddressMapper {

    private LegalDigitalAddressMapper(){}

    public static LegalDigitalAddressInt digitalToLegal(DigitalAddress legalDigitalAddress) {
        return LegalDigitalAddressInt.builder()
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.valueOf(legalDigitalAddress.getType()))
                .address(legalDigitalAddress.getAddress())
                .build();
    }

    public static DigitalAddress legalToDigital(LegalDigitalAddressInt digitalAddress) {
        return DigitalAddress.builder()
                .address(digitalAddress.getAddress())
                .type(digitalAddress.getType().getValue())
                .build();
    }
}
