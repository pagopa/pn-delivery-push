package it.pagopa.pn.deliverypush.sanitizers;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import org.owasp.html.PolicyFactory;
import org.owasp.html.HtmlPolicyBuilder;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that performs via the {@link #sanitize(Object)} method a cleanup of input parameters,
 *  allowing only determined HTML elements, based on the policies set in the constructor via the field {@link #policy}
 */
public abstract class HtmlSanitizer {

    private final PolicyFactory policy;

    public HtmlSanitizer() {
        this.policy = new HtmlPolicyBuilder().allowElements("").toFactory();
    }

    public Object sanitize(Object modelTemplate) {
        if(modelTemplate instanceof Map) {
            Map<String, Object> mapModelTemplate = (Map<String, Object>) modelTemplate;
            Map<String, Object> sanitizedMapModelTemplate = new HashMap<>(mapModelTemplate);
            return sanitize(sanitizedMapModelTemplate);
        }
        return modelTemplate;
    }

    /**
     *
     * @param untrustedHTML stringa contenente tag HTML di qualsiasi tipologia
     * @return una Stringa uguale a quella di input, ma che non contiene gli elementi HTMl non permessi.
     */
    public String sanitize(String untrustedHTML) {
        if(StringUtils.hasText(untrustedHTML)) {
            return policy.sanitize(untrustedHTML);
        }
        return untrustedHTML;
    }

    public NotificationRecipientInt sanitize(NotificationRecipientInt notificationRecipientInt) {
        if(notificationRecipientInt == null) return null;

        String sanitizedTrustedTaxId = sanitize(notificationRecipientInt.getTaxId());
        String sanitizedTrustedInternalId = sanitize(notificationRecipientInt.getInternalId());
        String sanitizedDenomination = sanitize(notificationRecipientInt.getDenomination());
        PhysicalAddressInt sanitizedPhysicalAddress = sanitize(notificationRecipientInt.getPhysicalAddress());
        NotificationPaymentInfoInt payment = notificationRecipientInt.getPayment();
        LegalDigitalAddressInt sanitizedDigitalDomicile = sanitize(notificationRecipientInt.getDigitalDomicile());

        return NotificationRecipientInt.builder()
                .taxId(sanitizedTrustedTaxId)
                .internalId(sanitizedTrustedInternalId)
                .denomination(sanitizedDenomination)
                .payment(payment)
                .physicalAddress(sanitizedPhysicalAddress)
                .digitalDomicile(notificationRecipientInt.getDigitalDomicile())
                .build();
    }

    private PhysicalAddressInt sanitize(PhysicalAddressInt physicalAddressInt) {
        if(physicalAddressInt == null) return null;

        String sanitizedAddress = sanitize(physicalAddressInt.getAddress());
        String sanitizedProvince = sanitize(physicalAddressInt.getProvince());
        String sanitizedZip = sanitize(physicalAddressInt.getZip());
        String sanitizedAt = sanitize(physicalAddressInt.getAt());
        String sanitizedAddressDetails = sanitize(physicalAddressInt.getAddressDetails());
        String sanitizedMunicipality = sanitize(physicalAddressInt.getMunicipality());
        String sanitizedForeignState = sanitize(physicalAddressInt.getForeignState());
        String sanitizedMunicipalityDetails = sanitize(physicalAddressInt.getMunicipalityDetails());

        return PhysicalAddressInt.builder()
                .address(sanitizedAddress)
                .province(sanitizedProvince)
                .zip(sanitizedZip)
                .at(sanitizedAt)
                .addressDetails(sanitizedAddressDetails)
                .municipality(sanitizedMunicipality)
                .foreignState(sanitizedForeignState)
                .municipalityDetails(sanitizedMunicipalityDetails)
                .build();

    }

    private LegalDigitalAddressInt sanitize(LegalDigitalAddressInt legalDigitalAddressInt) {
        if(legalDigitalAddressInt == null) return null;

        String sanitizedAddress = sanitize(legalDigitalAddressInt.getAddress());

        return LegalDigitalAddressInt.builder()
                .type(legalDigitalAddressInt.getType())
                .address(sanitizedAddress)
                .build();
    }

    public abstract Map<String, Object> sanitize(Map<String, Object> templateModelMap);

}
