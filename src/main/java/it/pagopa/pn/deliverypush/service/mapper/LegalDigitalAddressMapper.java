package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.LegalChannelType;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.LegalDigitalAddress;

public class LegalDigitalAddressMapper {

    public static DigitalAddress externalToInternal(LegalDigitalAddress legalDigitalAddress) {
        return DigitalAddress.builder()
                .type(DigitalAddress.TypeEnum.valueOf(legalDigitalAddress.getChannelType().getValue()))
                .address(legalDigitalAddress.getValue())
                .build();
    }

    public static LegalDigitalAddress internalToExternal(DigitalAddress digitalAddress) {
        LegalDigitalAddress legalDigitalAddress = new LegalDigitalAddress();
        legalDigitalAddress.setChannelType(
                LegalChannelType.valueOf(digitalAddress.getType().getValue())
            );
        legalDigitalAddress.setValue(digitalAddress.getAddress());
        
        return legalDigitalAddress;
    }
}
