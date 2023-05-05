package it.pagopa.pn.deliverypush.action.it.utils;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;


public class NotificationRecipientTestBuilder {
    private String taxId;
    private PhysicalAddressInt physicalAddress;
    private String internalId;
    private LegalDigitalAddressInt digitalDomicile;
    private NotificationPaymentInfoInt payment;
    private RecipientTypeInt recipientType;
    private String denomination;
    
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
    
    public NotificationRecipientTestBuilder withRecipientType(RecipientTypeInt recipientType) {
      this.recipientType = recipientType;
      return this;
    }

    public NotificationRecipientTestBuilder withDenomination(String denomination) {
        this.denomination = denomination;
        return this;
    }
    
    public NotificationRecipientInt build() {
        if(taxId == null){
            taxId = "generatedTestTaxId";
        }
        
        if(internalId == null){
            internalId = "ANON_"+taxId;
        }

        if(physicalAddress == null){
            physicalAddress = PhysicalAddressInt.builder()
                    .address("Test.address")
                    .at("Test.at")
                    .zip("Test.zip")
                    .foreignState("Test.foreignState")
                    .municipality("Test.municipality")
                    .addressDetails("Test.addressDetails")
                    .municipalityDetails("Test.municipalityDetails")
                    .province("Test.province")
                    .foreignState("Test.foreignState")
                    .build();
        }
        
        String denomination = "Name_and_surname_of_" + taxId;
        if(physicalAddress != null){
            physicalAddress.setFullname(denomination);
        }
        
        return NotificationRecipientInt.builder()
                .recipientType(RecipientTypeInt.PF)
                .taxId(taxId)
                .internalId(internalId)
                .denomination(denomination)
                .physicalAddress(physicalAddress)
                .digitalDomicile(digitalDomicile)
                .payment(payment)
                .build();
    }

}
