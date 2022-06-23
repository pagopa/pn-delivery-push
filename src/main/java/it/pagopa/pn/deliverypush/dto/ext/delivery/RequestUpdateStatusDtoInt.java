package it.pagopa.pn.deliverypush.dto.ext.delivery;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import lombok.*;

import javax.validation.constraints.Pattern;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class RequestUpdateStatusDtoInt {
    @Pattern( regexp = "[A-Za-z0-9-_]+")
    private String iun;
    private NotificationStatusInt nextState;
}
