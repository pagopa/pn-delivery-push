package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.LegalChannelType;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.LegalDigitalAddress;

public class LegalLegalDigitalAddressMapper {

    private LegalLegalDigitalAddressMapper(){}

    public static LegalDigitalAddressInt externalToInternal(LegalDigitalAddress legalDigitalAddress) {
        return LegalDigitalAddressInt.builder()
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.valueOf(legalDigitalAddress.getChannelType().getValue()))
                .address(legalDigitalAddress.getValue())
                .build();
    }

    public static LegalDigitalAddress internalToExternal(LegalDigitalAddressInt digitalAddress) {
        LegalDigitalAddress legalDigitalAddress = new LegalDigitalAddress();
        legalDigitalAddress.setChannelType(
                LegalChannelType.valueOf(digitalAddress.getType().getValue())
            );
        legalDigitalAddress.setValue(digitalAddress.getAddress());
        
        return legalDigitalAddress;
    }
}
