package it.pagopa.pn.deliverypush.action2.it.utils;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;


public class NotificationRecipientTestBuilder {
    private String taxId;
    private String internalId;
    private PhysicalAddress physicalAddress;
    private LegalDigitalAddressInt digitalDomicile;
    
    public static NotificationRecipientTestBuilder builder() {
        return new NotificationRecipientTestBuilder();
    }

    public NotificationRecipientTestBuilder withTaxId(String taxId) {
        this.taxId = taxId;
        return this;
    }

    public NotificationRecipientTestBuilder withInternalId(String internalId) {
        this.internalId = internalId;
        return this;
    }
    
    public NotificationRecipientTestBuilder withPhysicalAddress(PhysicalAddress physicalAddress) {
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
                .internalId(internalId)
                .denomination("Name_and_surname_of_" + taxId)
                .physicalAddress(physicalAddress)
                .digitalDomicile(digitalDomicile)
                .build();
    }

}
