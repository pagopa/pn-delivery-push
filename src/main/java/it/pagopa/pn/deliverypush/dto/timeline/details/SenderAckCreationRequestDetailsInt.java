package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SenderAckCreationRequestDetailsInt implements TimelineElementDetailsInt{
    private String legalFactId;

    @Override
    public String toLog() {
        return String.format(
                "legalFactId=%s",
                legalFactId
        );
    }
}
