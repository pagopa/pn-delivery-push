package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.AddressSQSMessageDigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.PhysicalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.PhysicalAddressSQSMessage;
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

    @Test
    void buildPublicRegistryValidationResponseTest() {
        String correlationId = "corrId";
        PhysicalAddress physicalAddress = getPhysicalAddress();

        PhysicalAddressSQSMessage physicalAddressMessage = new PhysicalAddressSQSMessage()
                .recIndex("1")
                .registry("Registry")
                .physicalAddress(physicalAddress);

        NationalRegistriesResponse expectedResponse = NationalRegistriesResponse.builder()
                .correlationId("corrId")
                .recIndex(1)
                .physicalAddress(PhysicalAddressInt.builder()
                        .address("Via Roma 1")
                        .zip("00100")
                        .province("RM")
                        .addressDetails("Dettagli")
                        .municipality("Roma")
                        .municipalityDetails("Dettagli Municipio")
                        .at("c/o")
                        .foreignState("Italia")
                        .build())
                .registry("Registry")
                .build();

        List<NationalRegistriesResponse> actualResponse = NationalRegistriesMessageUtil.buildPublicRegistryValidationResponse(correlationId, List.of(physicalAddressMessage));

        assertThat(actualResponse).containsExactly(expectedResponse);
    }

    @Test
    void buildPublicRegistryValidationResponseForNullPhysicalAddressTest() {
        String correlationId = "corrId1";
        PhysicalAddressSQSMessage physicalAddressMessage = new PhysicalAddressSQSMessage()
                .recIndex("1")
                .registry("Registry1")
                .physicalAddress(null);

        NationalRegistriesResponse expectedResponse = NationalRegistriesResponse.builder()
                .correlationId("corrId1")
                .recIndex(1)
                .physicalAddress(null)
                .registry("Registry1")
                .build();

        List<NationalRegistriesResponse> actualResponse = NationalRegistriesMessageUtil.buildPublicRegistryValidationResponse(correlationId, List.of(physicalAddressMessage));

        assertThat(actualResponse).containsExactly(expectedResponse);
    }

    @Test
    void buildPublicRegistryValidationResponseForEmptyAddressListTest() {
        String correlationId = "corrId1";

        List<NationalRegistriesResponse> actualResponse = NationalRegistriesMessageUtil.buildPublicRegistryValidationResponse(correlationId, List.of());

        assertThat(actualResponse).isEmpty();
    }

    private static PhysicalAddress getPhysicalAddress() {
        PhysicalAddress physicalAddress = new PhysicalAddress();
        physicalAddress.setAddress("Via Roma 1");
        physicalAddress.setZip("00100");
        physicalAddress.setProvince("RM");
        physicalAddress.setAddressDetails("Dettagli");
        physicalAddress.setMunicipality("Roma");
        physicalAddress.setMunicipalityDetails("Dettagli Municipio");
        physicalAddress.setAt("c/o");
        physicalAddress.setForeignState("Italia");
        return physicalAddress;
    }



}
