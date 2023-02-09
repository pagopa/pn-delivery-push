package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressSQSMessageDigitalAddress;
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
        PublicRegistryResponse expectedResponse = PublicRegistryResponse.builder()
                .correlationId("corrId1")
                .digitalAddress(LegalDigitalAddressInt.builder().type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).address("prova@pec.it").build())
                .build();

        PublicRegistryResponse actualResponse = NationalRegistriesMessageUtil.buildPublicRegistryResponse(correlationId, List.of(digitalAddressMessage));

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void buildPublicRegistryResponseForNullDigitalAddressTest() {
        String correlationId = "corrId1";
        PublicRegistryResponse expectedResponse = PublicRegistryResponse.builder()
                .correlationId("corrId1")
                .digitalAddress(null)
                .build();

        PublicRegistryResponse actualResponse = NationalRegistriesMessageUtil.buildPublicRegistryResponse(correlationId, null);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

}
