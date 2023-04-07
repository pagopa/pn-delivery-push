package it.pagopa.pn.deliverypush.dto.timeline;

import lombok.*;

@Getter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class NotificationRefusedErrorInt {
    private String errorCode;
    private String detail;
}
