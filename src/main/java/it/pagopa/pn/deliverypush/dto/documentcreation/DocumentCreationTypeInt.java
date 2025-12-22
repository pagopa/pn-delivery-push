package it.pagopa.pn.deliverypush.dto.documentcreation;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import lombok.Getter;

@Getter
public enum DocumentCreationTypeInt {

    RECIPIENT_ACCESS(LegalFactCategoryInt.RECIPIENT_ACCESS.getValue());
    private final String value;

    DocumentCreationTypeInt(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}