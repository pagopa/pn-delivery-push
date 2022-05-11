package it.pagopa.pn.deliverypush.action2.it.utils;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;


public class NotificationRecipientTestBuilder {
    private String taxId;
    private PhysicalAddress physicalAddress;
    private DigitalAddress digitalDomicile;

    public static NotificationRecipientTestBuilder builder() {
        return new NotificationRecipientTestBuilder();
    }

    public NotificationRecipientTestBuilder withTaxId(String taxId) {
        this.taxId = taxId;
        return this;
    }

    public NotificationRecipientTestBuilder withPhysicalAddress(PhysicalAddress physicalAddress) {
        this.physicalAddress = physicalAddress;
        return this;
    }

    public NotificationRecipientTestBuilder withDigitalDomicile(DigitalAddress digitalDomicile) {
        this.digitalDomicile = digitalDomicile;
        return this;
    }

    public NotificationRecipientInt build() {
        return NotificationRecipientInt.builder()
                .taxId(taxId)
                .physicalAddress(physicalAddress)
                .digitalDomicile(digitalDomicile)
                .build();
    }

}
