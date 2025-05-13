package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.AddressSQSMessageDigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.PhysicalAddressResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.PhysicalAddressesResponse;
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

    public static List<NationalRegistriesResponse> buildPublicRegistryValidationResponse(PhysicalAddressesResponse physicalAddressesResponse) {
        String correlationId = physicalAddressesResponse.getCorrelationId();
        List<PhysicalAddressResponse> addresses = physicalAddressesResponse.getAddresses();

        return addresses.stream()
                .map(address -> NationalRegistriesResponse.builder()
                        .correlationId(correlationId)
                        .recIndex(address.getRecIndex())
                        .physicalAddress(mapToPhysicalAddressInt(address))
                        .registry(address.getRegistry())
                        .error(address.getError())
                        .errorStatus(address.getErrorStatus())
                        .build())
                .toList();
    }

    private static PhysicalAddressInt mapToPhysicalAddressInt(PhysicalAddressResponse message) {
        if (message.getPhysicalAddress() != null) {
            return PhysicalAddressInt.builder()
                    .address(message.getPhysicalAddress().getAddress())
                    .zip(message.getPhysicalAddress().getZip())
                    .province(message.getPhysicalAddress().getProvince())
                    .addressDetails(message.getPhysicalAddress().getAddressDetails())
                    .municipality(message.getPhysicalAddress().getMunicipality())
                    .municipalityDetails(message.getPhysicalAddress().getMunicipalityDetails())
                    .at(message.getPhysicalAddress().getAt())
                    .foreignState(message.getPhysicalAddress().getForeignState())
                    .build();
        }
        return null;
    }

}
