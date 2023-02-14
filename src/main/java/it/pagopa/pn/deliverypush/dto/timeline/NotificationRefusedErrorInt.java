package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import lombok.*;

@Getter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class NotificationRefusedErrorInt {
    private PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt errorCode;
    private String detail;
}
