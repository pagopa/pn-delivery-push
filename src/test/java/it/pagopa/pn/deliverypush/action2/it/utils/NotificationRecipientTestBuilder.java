package it.pagopa.pn.deliverypush.action2.it.utils;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;


public class NotificationRecipientTestBuilder {
    private String taxId;
    private PhysicalAddressInt physicalAddress;
    private LegalDigitalAddressInt digitalDomicile;

    public static NotificationRecipientTestBuilder builder() {
        return new NotificationRecipientTestBuilder();
    }

    public NotificationRecipientTestBuilder withTaxId(String taxId) {
        this.taxId = taxId;
        return this;
    }

    public NotificationRecipientTestBuilder withPhysicalAddress(PhysicalAddressInt physicalAddress) {
        this.physicalAddress = physicalAddress;
        return this;
    }

    public NotificationRecipientTestBuilder withDigitalDomicile(LegalDigitalAddressInt digitalDomicile) {
        this.digitalDomicile = digitalDomicile;
        return this;
    }

    public NotificationRecipientInt build() {
        return NotificationRecipientInt.builder()
                .taxId(taxId)
                .denomination("Name_and_surname_of_" + taxId)
                .physicalAddress(physicalAddress)
                .digitalDomicile(digitalDomicile)
                .build();
    }

}
