package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressSQSMessageDigitalAddress;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class NationalRegistriesMessageUtil {

    private NationalRegistriesMessageUtil(){}

    public static NationalRegistriesResponse buildPublicRegistryResponse(String correlationId, List<AddressSQSMessageDigitalAddress> digitalAddresses) {
        return NationalRegistriesResponse.builder()
                .correlationId(correlationId)
                .digitalAddress(mapToLegalDigitalAddressInt(digitalAddresses))
                .build();
    }

    private static LegalDigitalAddressInt mapToLegalDigitalAddressInt(List<AddressSQSMessageDigitalAddress> digitalAddresses) {
        if(CollectionUtils.isEmpty(digitalAddresses)) return null;

        return LegalDigitalAddressInt.builder()
                .address(digitalAddresses.get(0).getAddress())
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.valueOf(digitalAddresses.get(0).getType()))
                .build();
    }

}
