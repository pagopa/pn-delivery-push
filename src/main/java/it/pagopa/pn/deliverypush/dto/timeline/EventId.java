package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ContactPhase;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DeliveryMode;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import lombok.*;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class EventId {
    private String iun;
    private Integer recIndex;
    private DigitalAddressSource source;
    private int index;
    private ContactPhase contactPhase;
    private int sentAttemptMade;
    private DeliveryMode deliveryMode;
}
