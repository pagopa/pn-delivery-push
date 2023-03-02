package it.pagopa.pn.deliverypush.dto.io;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;

import java.time.Instant;

@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class IoSendMessageInfo {
    private Result result;
    private Instant senDate;

    public enum Result {
        NOT_SENT_APPIO_UNAVAILABLE("NOT_SENT_APPIO_UNAVAILABLE"),

        NOT_SENT_OPTIN_ALREADY_SENT("NOT_SENT_OPTIN_ALREADY_SENT"),

        NOT_SENT_OPTIN_DISABLED_BY_CONF("NOT_SENT_OPTIN_DISABLED_BY_CONF"),

        NOT_SENT_COURTESY_DISABLED_BY_CONF("NOT_SENT_COURTESY_DISABLED_BY_CONF"),

        SENT_COURTESY("SENT_COURTESY"),

        SENT_OPTIN("SENT_OPTIN"),

        ERROR_USER_STATUS("ERROR_USER_STATUS"),

        ERROR_COURTESY("ERROR_COURTESY"),

        ERROR_OPTIN("ERROR_OPTIN");

        private final String value;

        Result(String value) {
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
