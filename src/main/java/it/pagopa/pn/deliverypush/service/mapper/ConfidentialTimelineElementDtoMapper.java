package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.AddressDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.AnalogDomicile;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.ConfidentialTimelineElementDto;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;

public class ConfidentialTimelineElementDtoMapper {
    private ConfidentialTimelineElementDtoMapper(){};
    
    public static ConfidentialTimelineElementDto internalToExternal(ConfidentialTimelineElementDtoInt dtoInt){
        ConfidentialTimelineElementDto.ConfidentialTimelineElementDtoBuilder dtoExtBuilder = ConfidentialTimelineElementDto.builder()
                .timelineElementId(dtoInt.getTimelineElementId());
        
        if(dtoInt.getDigitalAddress() != null){
            dtoExtBuilder.digitalAddress(
                    AddressDto.builder()
                            .value(dtoInt.getDigitalAddress())
                            .build()
            );
        }

        PhysicalAddress physicalAddressInt = dtoInt.getPhysicalAddress();
        if (physicalAddressInt != null){
            dtoExtBuilder.physicalAddress(
                    AnalogDomicile.builder()
                            .address(physicalAddressInt.getAddress())
                            .addressDetails(physicalAddressInt.getAddressDetails())
                            .at(physicalAddressInt.getAt())
                            .municipality(physicalAddressInt.getMunicipality())
                            .cap(physicalAddressInt.getZip())
                            .state(physicalAddressInt.getForeignState())
                            .province(physicalAddressInt.getProvince())
                            .build()
            );
        }
        
        return dtoExtBuilder.build();
    }

    public static ConfidentialTimelineElementDtoInt externalToInternal(ConfidentialTimelineElementDto dtoExt){
        ConfidentialTimelineElementDtoInt.ConfidentialTimelineElementDtoIntBuilder dtoIntBuilder = ConfidentialTimelineElementDtoInt.builder()
                .timelineElementId(dtoExt.getTimelineElementId());

        if(dtoExt.getDigitalAddress() != null){
            dtoIntBuilder.digitalAddress(dtoExt.getDigitalAddress().getValue());
        }

        AnalogDomicile analogDomicile = dtoExt.getPhysicalAddress();
        if (analogDomicile != null){
            dtoIntBuilder.physicalAddress(
                PhysicalAddress.builder()
                        .address(analogDomicile.getAddress())
                        .addressDetails(analogDomicile.getAddressDetails())
                        .at(analogDomicile.getAt())
                        .municipality(analogDomicile.getMunicipality())
                        .zip(analogDomicile.getCap())
                        .foreignState(analogDomicile.getState())
                        .province(analogDomicile.getProvince())
                        .build()
            );
        }
        
        return dtoIntBuilder.build();
    }
}
