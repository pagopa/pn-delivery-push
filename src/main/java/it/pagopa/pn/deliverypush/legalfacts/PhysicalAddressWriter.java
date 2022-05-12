package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PhysicalAddressWriter {

    public String nullSafePhysicalAddressToString(PhysicalAddress physicalAddress, String recipientDenomination, String separator ) {
        String result = null;

        if ( recipientDenomination != null && physicalAddress != null) {

            List<String> standardAddressString = physicalAddress.toStandardAddressString( recipientDenomination );
            if ( standardAddressString != null ) {
                result = String.join( separator, standardAddressString );
            }
        }

        return result;
    }
}
