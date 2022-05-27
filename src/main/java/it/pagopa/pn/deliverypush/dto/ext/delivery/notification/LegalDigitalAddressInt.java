package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString
public class LegalDigitalAddressInt extends DigitalAddressInt{


    public enum LEGAL_DIGITAL_ADDRESS_TYPE{
        PEC("PEC"),
        APPIO("APPIO");

        private final String value;

        LEGAL_DIGITAL_ADDRESS_TYPE(String value) {
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

    private LEGAL_DIGITAL_ADDRESS_TYPE type;
}
