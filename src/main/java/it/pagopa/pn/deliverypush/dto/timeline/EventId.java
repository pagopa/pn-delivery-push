package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
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
    private Integer sentAttemptMade;
    private Integer progressIndex;
    private DocumentCreationTypeInt documentCreationType;
    private String creditorTaxId;
    private String noticeCode;
    private Boolean isFirstSendRetry;
    private String relatedTimelineId;
    private Boolean optin;
}
