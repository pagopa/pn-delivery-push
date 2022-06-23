package it.pagopa.pn.deliverypush.dto.address;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString
@SuperBuilder( toBuilder = true )
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
