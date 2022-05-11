package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import lombok.Builder;
import lombok.Getter;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;

@Getter
public class TimelineElementInternal extends TimelineElement {
    private final String iun;

    @Builder(builderMethodName = "timelineInternalBuilder")
    public TimelineElementInternal(String elementId, Date timestamp, @Valid List<LegalFactsId> legalFactsIds, TimelineElementCategory category, TimelineElementDetails details, String iun) {
        super(elementId, timestamp, legalFactsIds, category, details);
        this.iun = iun;
    }

}
