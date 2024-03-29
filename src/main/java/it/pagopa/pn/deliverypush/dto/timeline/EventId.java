package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


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
    private ContactPhaseInt contactPhase;
    private Integer sentAttemptMade;
    private DeliveryModeInt deliveryMode;
    private Integer progressIndex;
    private DocumentCreationTypeInt documentCreationType;
    private CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT courtesyAddressType;
    private String creditorTaxId;
    private String noticeCode;
    private Boolean isFirstSendRetry;
    private String relatedTimelineId;
    private Boolean optin;
}
