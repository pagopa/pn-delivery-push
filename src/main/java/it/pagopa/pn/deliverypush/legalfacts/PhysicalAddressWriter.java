package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PhysicalAddressWriter {

    public String nullSafePhysicalAddressToString(PhysicalAddress physicalAddress, String recipientDenomination, String separator ) {
        String result = null;

        if ( recipientDenomination != null && physicalAddress != null) {

            List<String> standardAddressString = toStandardAddressString( recipientDenomination, physicalAddress );
            result = String.join( separator, standardAddressString );
        }

        return result;
    }

    private List<String> toStandardAddressString(String recipientDenomination , PhysicalAddress physicalAddress) {
        List<String> standardAddressString = new ArrayList<>();

        standardAddressString.add( recipientDenomination );

        if ( isNotBlank( physicalAddress.getAt() ) ) {
            standardAddressString.add( physicalAddress.getAt() );
        }

        if ( isNotBlank( physicalAddress.getAddressDetails() ) ) {
            standardAddressString.add( physicalAddress.getAddressDetails() );
        }

        standardAddressString.add( physicalAddress.getAddress() );
        standardAddressString.add( physicalAddress.getZip() + " " + physicalAddress.getMunicipality() + " " + physicalAddress.getProvince() );

        if ( isNotBlank( physicalAddress.getForeignState() ) ) {
            standardAddressString.add( physicalAddress.getForeignState() );
        }

        return standardAddressString;
    }

    private boolean isNotBlank( String str) {
        return str != null && !str.isBlank();
    }
}
