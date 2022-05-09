package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class TimelineElementInternal extends TimelineElement {
    private String iun;

    public TimelineElementInternal(TimelineElement element, String iun) {
        super(element.getElementId(), element.getTimestamp(), element.getLegalFactsIds(), element.getCategory(), element.getDetails());
        this.iun = iun;
    }

}
