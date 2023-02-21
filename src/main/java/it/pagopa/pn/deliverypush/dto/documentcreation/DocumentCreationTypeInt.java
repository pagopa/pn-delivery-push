package it.pagopa.pn.deliverypush.dto.documentcreation;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentCategory;

public enum DocumentCreationTypeInt {
    AAR(DocumentCategory.AAR.getValue()),

    SENDER_ACK(LegalFactCategoryInt.SENDER_ACK.getValue()),

    DIGITAL_DELIVERY(LegalFactCategoryInt.DIGITAL_DELIVERY.getValue()),

    RECIPIENT_ACCESS(LegalFactCategoryInt.RECIPIENT_ACCESS.getValue());

    private final String value;

    DocumentCreationTypeInt(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}