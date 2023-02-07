package it.pagopa.pn.deliverypush.dto.nationalregistries;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class CheckTaxIdOKInt {
    private String taxId;
    private Boolean isValid;
    private ErrorCodeEnumInt errorCode;

    public enum ErrorCodeEnumInt {
        ERR01("B001_CHECK_TAX_ID_ERR01"),

        ERR02("B001_CHECK_TAX_ID_ERR02"),

        ERR03("B001_CHECK_TAX_ID_ERR03");

        private String value;

        ErrorCodeEnumInt(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
        
    }
}
