package it.pagopa.pn.deliverypush.dto.cancellation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class StatusDetailInt {

    private String code;
    private String level;
    private String detail;
}

