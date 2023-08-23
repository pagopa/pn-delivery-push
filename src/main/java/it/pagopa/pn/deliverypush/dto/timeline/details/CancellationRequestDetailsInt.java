package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class CancellationRequestDetailsInt implements RecipientRelatedTimelineElementDetails {

    private int recIndex;

    public String toLog() {
        return this.toString();
    }
}
