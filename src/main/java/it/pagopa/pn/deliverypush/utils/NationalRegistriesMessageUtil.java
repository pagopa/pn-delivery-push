package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressSQSMessageDigitalAddress;

public class NationalRegistriesMessageUtil {

    private NationalRegistriesMessageUtil(){}

    public static PublicRegistryResponse buildPublicRegistryResponse(String correlationId, AddressSQSMessageDigitalAddress digitalAddress) {
        return PublicRegistryResponse.builder()
                .correlationId(correlationId)
                .digitalAddress(mapToLegalDigitalAddressInt(digitalAddress))
                .build();
    }

    private static LegalDigitalAddressInt mapToLegalDigitalAddressInt(AddressSQSMessageDigitalAddress digitalAddress) {
        return LegalDigitalAddressInt.builder()
                .address(digitalAddress.getAddress())
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.valueOf(digitalAddress.getType()))
                .build();
    }

}
