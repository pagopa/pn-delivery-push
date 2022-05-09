package it.pagopa.pn.deliverypush.dto.ext.delivery;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatus;
import lombok.*;

import javax.validation.constraints.Pattern;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class RequestUpdateStatusDto {

    @Pattern( regexp = "[A-Za-z0-9-_]+")
    private String iun;

    private NotificationStatus nextState;
}
