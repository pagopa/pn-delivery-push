package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
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
    private DigitalAddressSourceInt source;
    private int index;
    private ContactPhaseInt contactPhase;
    private int sentAttemptMade;
    private DeliveryModeInt deliveryMode;
    private int progressIndex;
    private DocumentCreationTypeInt documentCreationType;
}
