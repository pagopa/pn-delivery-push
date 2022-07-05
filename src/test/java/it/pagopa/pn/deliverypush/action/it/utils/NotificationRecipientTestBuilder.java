package it.pagopa.pn.deliverypush.action.it.utils;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;


public class NotificationRecipientTestBuilder {
    private String taxId;
    private PhysicalAddressInt physicalAddress;
    private String internalId;
    private LegalDigitalAddressInt digitalDomicile;
    private NotificationPaymentInfoInt payment;
    
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

    public NotificationRecipientTestBuilder withInternalId(String internalId) {
        this.internalId = internalId;
        return this;
    }

    public NotificationRecipientTestBuilder withDigitalDomicile(LegalDigitalAddressInt digitalDomicile) {
        this.digitalDomicile = digitalDomicile;
        return this;
    }
    

    public NotificationRecipientTestBuilder withPayment(NotificationPaymentInfoInt payment) {
        this.payment = payment;
        return this;
    }
    
    public NotificationRecipientInt build() {
        return NotificationRecipientInt.builder()
                .taxId(taxId)
                .internalId(internalId)
                .denomination("Name_and_surname_of_" + taxId)
                .physicalAddress(physicalAddress)
                .digitalDomicile(digitalDomicile)
                .payment(payment)
                .build();
    }

}
