package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.AddressSQSMessageDigitalAddress;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NationalRegistriesMessageUtilTest {


    @Test
    void buildPublicRegistryResponseTest() {
        String correlationId = "corrId1";
        AddressSQSMessageDigitalAddress digitalAddressMessage = new AddressSQSMessageDigitalAddress()
                .address("prova@pec.it")
                .type("PEC")
                .recipient(AddressSQSMessageDigitalAddress.RecipientEnum.PERSONA_FISICA);
        NationalRegistriesResponse expectedResponse = NationalRegistriesResponse.builder()
                .correlationId("corrId1")
                .digitalAddress(LegalDigitalAddressInt.builder().type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).address("prova@pec.it").build())
                .build();

        NationalRegistriesResponse actualResponse = NationalRegistriesMessageUtil.buildPublicRegistryResponse(correlationId, List.of(digitalAddressMessage));

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void buildPublicRegistryResponseForNullDigitalAddressTest() {
        String correlationId = "corrId1";
        NationalRegistriesResponse expectedResponse = NationalRegistriesResponse.builder()
                .correlationId("corrId1")
                .digitalAddress(null)
                .build();

        NationalRegistriesResponse actualResponse = NationalRegistriesMessageUtil.buildPublicRegistryResponse(correlationId, null);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

}
