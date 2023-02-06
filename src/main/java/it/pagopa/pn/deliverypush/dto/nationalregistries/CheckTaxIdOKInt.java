package it.pagopa.pn.deliverypush.dto.nationalregistries;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.CheckTaxIdOK;
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

        @JsonCreator
        public static CheckTaxIdOK.ErrorCodeEnum fromValue(String value) {
            for (CheckTaxIdOK.ErrorCodeEnum b : CheckTaxIdOK.ErrorCodeEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }
}
