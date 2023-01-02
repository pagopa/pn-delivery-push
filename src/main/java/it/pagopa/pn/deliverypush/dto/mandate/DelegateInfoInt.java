package it.pagopa.pn.deliverypush.dto.mandate;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class DelegateInfoInt {
    private String internalId;
    private String operatorUuid;
    private String mandateId;
    private String denomination;
    private DelegateType delegateType;

    public enum DelegateType {
        PF("PF"),

        PG("PG");

        private final String value;

        DelegateType(String value) {
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
}
