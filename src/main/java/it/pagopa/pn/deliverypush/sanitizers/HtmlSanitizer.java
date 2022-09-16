package it.pagopa.pn.deliverypush.sanitizers;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class HtmlSanitizer {

    private final PolicyFactory policy;

    public HtmlSanitizer() {
        this.policy = Sanitizers.IMAGES;
    }

    public Object sanitize(Object modelTemplate) {
        if(modelTemplate instanceof Map) {
            Map<String, Object> mapModelTemplate = (Map<String, Object>) modelTemplate;
            Map<String, Object> trustedMapModelTemplate = new HashMap<>(mapModelTemplate);
            return sanitize(trustedMapModelTemplate);
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
        String trustedTaxId = sanitize(notificationRecipientInt.getTaxId());
        String trustedInternalId = sanitize(notificationRecipientInt.getInternalId());
        String trustedDenomination = sanitize(notificationRecipientInt.getDenomination());
        NotificationPaymentInfoInt payment = notificationRecipientInt.getPayment();
        PhysicalAddressInt physicalAddress = notificationRecipientInt.getPhysicalAddress();
        LegalDigitalAddressInt digitalDomicile = notificationRecipientInt.getDigitalDomicile();

        return NotificationRecipientInt.builder()
                .taxId(trustedTaxId)
                .internalId(trustedInternalId)
                .denomination(trustedDenomination)
                .payment(payment)
                .physicalAddress(physicalAddress)
                .digitalDomicile(digitalDomicile)
                .build();
    }

    public abstract Map<String, Object> sanitize(Map<String, Object> templateModelMap);

}
