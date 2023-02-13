package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationRefusedErrorCodeInt;
import lombok.*;

@Getter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class NotificationRefusedErrorInt {
    private NotificationRefusedErrorCodeInt errorCode;
    private String detail;
}
