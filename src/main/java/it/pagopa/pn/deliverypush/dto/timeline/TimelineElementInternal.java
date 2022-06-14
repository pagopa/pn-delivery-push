package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = true)
public class TimelineElementInternal extends TimelineElement {
    private final String iun;
    
    private final String padId;
    private final String foreignState;
    private final Integer numberOfPages;
    
    @Builder(builderMethodName = "timelineInternalBuilder")
    public TimelineElementInternal(String elementId,
                                   Instant timestamp,
                                   List<LegalFactsId> legalFactsIds,
                                   TimelineElementCategory category,
                                   TimelineElementDetails details,
                                   String iun,
                                   String padId, String foreignState, Integer numberOfPages) {
        super(elementId, timestamp, legalFactsIds, category, details);
        this.iun = iun;
        this.padId = padId;
        this.foreignState = foreignState;
        this.numberOfPages = numberOfPages;
    }

    public TimelineElementInternal(String iun, TimelineElement el, String padId, String foreignState, Integer numberOfPages) {
        super(el.getElementId(), el.getTimestamp(), el.getLegalFactsIds(), el.getCategory(), el.getDetails());
        this.iun = iun;
        this.padId = padId;
        this.foreignState = foreignState;
        this.numberOfPages = numberOfPages;
    }
}
