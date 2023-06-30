package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.AddressDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.AnalogDomicile;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.NotificationRecipientAddressesDtoInt;

public class NotificationRecipientAddressesDtoMapper {
    private NotificationRecipientAddressesDtoMapper(){}
    
    public static NotificationRecipientAddressesDto internalToExternal(NotificationRecipientAddressesDtoInt dtoInt) {
        NotificationRecipientAddressesDto dtoExt = new NotificationRecipientAddressesDto();
        dtoExt.setDenomination(dtoInt.getDenomination());
        dtoExt.setDigitalAddress(getAddressDtoFromDigitalAddress(dtoInt.getDigitalAddress()));
        dtoExt.setPhysicalAddress(getAnalogDomicileFromPhysical(dtoInt.getPhysicalAddress()));
        return dtoExt;
    }

    private static AddressDto getAddressDtoFromDigitalAddress(DigitalAddressInt digitalAddressInt){
        AddressDto addressDtoExt = null;
        if(digitalAddressInt != null ){
            addressDtoExt = new AddressDto();
            addressDtoExt.setValue(digitalAddressInt.getAddress());
        }
        return addressDtoExt;
    }

    private static AnalogDomicile getAnalogDomicileFromPhysical(PhysicalAddressInt physicalAddress){
        AnalogDomicile address = null;
        if(physicalAddress != null){
            address = new AnalogDomicile();
            address.setAddress(physicalAddress.getAddress());
            address.setAt(physicalAddress.getAt());
            address.setAddressDetails(physicalAddress.getAddressDetails());
            address.setCap(physicalAddress.getZip());
            address.setMunicipality(physicalAddress.getMunicipality());
            address.setMunicipalityDetails(physicalAddress.getMunicipalityDetails());
            address.setProvince(physicalAddress.getProvince());
            address.setState(physicalAddress.getForeignState());
        }
        return address;
    }

}
