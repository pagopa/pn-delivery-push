package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.AddressSQSMessageDigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.PhysicalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.PhysicalAddressResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.PhysicalAddressesResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;
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

    @Test
    void buildPublicRegistryValidationResponseWithValidAddresses() {
        PhysicalAddressesResponse physicalAddressesResponse = new PhysicalAddressesResponse()
                .correlationId("corrId1")
                .addresses(List.of(
                        new PhysicalAddressResponse()
                                .recIndex(0)
                                .physicalAddress(new PhysicalAddress()
                                        .address("Via Roma 1")
                                        .zip("00100")
                                        .province("RM")
                                        .municipality("Roma"))
                                .registry("Registry1")
                ));

        List<NationalRegistriesResponse> responses = NationalRegistriesMessageUtil.buildPublicRegistryValidationResponse(physicalAddressesResponse);

        assertThat(responses).hasSize(1);
        NationalRegistriesResponse response = responses.get(0);
        assertThat(response.getCorrelationId()).isEqualTo("corrId1");
        assertThat(response.getRecIndex()).isEqualTo(0);
        assertThat(response.getPhysicalAddress()).isNotNull();
        assertThat(response.getPhysicalAddress().getAddress()).isEqualTo("Via Roma 1");
        assertThat(response.getPhysicalAddress().getZip()).isEqualTo("00100");
        assertThat(response.getPhysicalAddress().getProvince()).isEqualTo("RM");
        assertThat(response.getPhysicalAddress().getMunicipality()).isEqualTo("Roma");
        assertThat(response.getRegistry()).isEqualTo("Registry1");
    }

    @Test
    void buildPublicRegistryValidationResponseWithNoAddresses() {
        PhysicalAddressesResponse physicalAddressesResponse = new PhysicalAddressesResponse()
                .correlationId("corrId2")
                .addresses(Collections.emptyList());

        List<NationalRegistriesResponse> responses = NationalRegistriesMessageUtil.buildPublicRegistryValidationResponse(physicalAddressesResponse);

        assertThat(responses).isEmpty();
    }

    @Test
    void buildPublicRegistryValidationResponseWithError() {
        PhysicalAddressesResponse physicalAddressesResponse = new PhysicalAddressesResponse()
                .correlationId("corrId3")
                .addresses(List.of(
                        new PhysicalAddressResponse()
                                .recIndex(1)
                                .error("Address not found")
                                .errorStatus(404)
                ));

        List<NationalRegistriesResponse> responses = NationalRegistriesMessageUtil.buildPublicRegistryValidationResponse(physicalAddressesResponse);

        assertThat(responses).hasSize(1);
        NationalRegistriesResponse response = responses.get(0);
        assertThat(response.getCorrelationId()).isEqualTo("corrId3");
        assertThat(response.getRecIndex()).isEqualTo(1);
        assertThat(response.getPhysicalAddress()).isNull();
        assertThat(response.getError()).isEqualTo("Address not found");
        assertThat(response.getErrorStatus()).isEqualTo(404);
    }

}
