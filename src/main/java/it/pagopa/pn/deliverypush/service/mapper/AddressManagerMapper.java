package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.AnalogAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.NormalizeItemsResult;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeItemsResultInt;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeResultInt;

import java.util.ArrayList;
import java.util.List;

public class AddressManagerMapper {
    private AddressManagerMapper(){}
    
    public static NormalizeItemsResultInt externalToInternal(NormalizeItemsResult response) {
        NormalizeItemsResultInt.NormalizeItemsResultIntBuilder normalizeItemsResultBuilder = NormalizeItemsResultInt.builder()
                        .correlationId(response.getCorrelationId());
        
        List<NormalizeResultInt> resultItems = new ArrayList<>();

        response.getResultItems().forEach( normalizeResultResponse ->{
            NormalizeResultInt.NormalizeResultIntBuilder normalizeResultBuilder = NormalizeResultInt.builder();
            normalizeResultBuilder.id(normalizeResultResponse.getId());
            normalizeResultBuilder.error(normalizeResultResponse.getError());

            if(normalizeResultResponse.getNormalizedAddress() != null){
                PhysicalAddressInt physicalAddressInt = getPhysicalFromAnalog(normalizeResultResponse.getNormalizedAddress());
                normalizeResultBuilder.normalizedAddress(physicalAddressInt);
            }

            NormalizeResultInt normalizeResultInt = normalizeResultBuilder.build();
            resultItems.add(normalizeResultInt);
        });

        normalizeItemsResultBuilder.resultItems(resultItems);
        
        return normalizeItemsResultBuilder.build();
    }

    public static AnalogAddress getAnalogAddressFromPhysical(PhysicalAddressInt physicalAddress){
        AnalogAddress address = new AnalogAddress();
        address.setAddressRow(physicalAddress.getAddress());
        address.setAddressRow2(physicalAddress.getAddressDetails());
        address.setCap(physicalAddress.getZip());
        address.setCity(physicalAddress.getMunicipality());
        address.setCity2(physicalAddress.getMunicipalityDetails());
        address.setPr(physicalAddress.getProvince());
        address.setCountry(physicalAddress.getForeignState());

        return address;
    }


    private static PhysicalAddressInt getPhysicalFromAnalog(AnalogAddress analogAddress){
        return PhysicalAddressInt.builder()
                .address(analogAddress.getAddressRow())
                .addressDetails(analogAddress.getAddressRow2())
                .zip(analogAddress.getCap())
                .municipality(analogAddress.getCity())
                .municipalityDetails(analogAddress.getCity2())
                .province(analogAddress.getPr())
                .foreignState(analogAddress.getCountry())
                .build();
    }

}
