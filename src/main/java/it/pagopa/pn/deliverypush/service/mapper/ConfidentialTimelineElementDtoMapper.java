package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.AddressDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.AnalogDomicile;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.ConfidentialTimelineElementDto;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;

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

        PhysicalAddressInt physicalAddressInt = dtoInt.getPhysicalAddress();
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

        PhysicalAddressInt newPhysicalAddressInt = dtoInt.getNewPhysicalAddress();
        if (newPhysicalAddressInt != null){
            dtoExtBuilder.newPhysicalAddress(
                    AnalogDomicile.builder()
                            .address(newPhysicalAddressInt.getAddress())
                            .addressDetails(newPhysicalAddressInt.getAddressDetails())
                            .at(newPhysicalAddressInt.getAt())
                            .municipality(newPhysicalAddressInt.getMunicipality())
                            .cap(newPhysicalAddressInt.getZip())
                            .state(newPhysicalAddressInt.getForeignState())
                            .province(newPhysicalAddressInt.getProvince())
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

        AnalogDomicile physicalAddress = dtoExt.getPhysicalAddress();
        if (physicalAddress != null){
            dtoIntBuilder.physicalAddress(
                PhysicalAddressInt.builder()
                        .address(physicalAddress.getAddress())
                        .addressDetails(physicalAddress.getAddressDetails())
                        .at(physicalAddress.getAt())
                        .municipality(physicalAddress.getMunicipality())
                        .zip(physicalAddress.getCap())
                        .foreignState(physicalAddress.getState())
                        .province(physicalAddress.getProvince())
                        .build()
            );
        }

        AnalogDomicile newPhysicalAddress = dtoExt.getNewPhysicalAddress();
        if (newPhysicalAddress != null){
            dtoIntBuilder.newPhysicalAddress(
                    PhysicalAddressInt.builder()
                            .address(newPhysicalAddress.getAddress())
                            .addressDetails(newPhysicalAddress.getAddressDetails())
                            .at(newPhysicalAddress.getAt())
                            .municipality(newPhysicalAddress.getMunicipality())
                            .zip(newPhysicalAddress.getCap())
                            .foreignState(newPhysicalAddress.getState())
                            .province(newPhysicalAddress.getProvince())
                            .build()
            );
        }
        
        return dtoIntBuilder.build();
    }
}
