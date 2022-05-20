package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class DigitalAddressEntity {
    private TypeEnum type;
    
    public enum TypeEnum {
        PEC("PEC");

        private String value;

        TypeEnum(String value) {
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
