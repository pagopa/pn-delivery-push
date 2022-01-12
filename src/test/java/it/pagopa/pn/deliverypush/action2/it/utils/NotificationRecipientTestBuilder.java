package it.pagopa.pn.deliverypush.action2.it.utils;

import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;

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

    public NotificationRecipient build() {
        return NotificationRecipient.builder()
                .taxId(taxId)
                .physicalAddress(physicalAddress)
                .digitalDomicile(digitalDomicile)
                .build();
    }

}
