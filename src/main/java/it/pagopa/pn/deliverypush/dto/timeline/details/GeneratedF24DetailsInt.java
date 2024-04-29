package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder( toBuilder = true )
@EqualsAndHashCode
@ToString
public class GeneratedF24DetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private List<String> f24Attachments;

    @Override
    public String toLog() {
        return String.format(
                "recIndex=%d",
                recIndex
        );
    }
}