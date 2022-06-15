package it.pagopa.pn.deliverypush.dto.address;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuperBuilder
public class CourtesyDigitalAddressInt extends DigitalAddressInt{

    public enum COURTESY_DIGITAL_ADDRESS_TYPE{
        EMAIL("EMAIL"),
        SMS("SMS"),
        APPIO("APPIO");

        private final String value;

        COURTESY_DIGITAL_ADDRESS_TYPE(String value) {
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

    private COURTESY_DIGITAL_ADDRESS_TYPE type;

}
