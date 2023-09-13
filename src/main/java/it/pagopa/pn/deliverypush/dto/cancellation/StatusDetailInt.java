package it.pagopa.pn.deliverypush.dto.cancellation;

import lombok.*;

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

