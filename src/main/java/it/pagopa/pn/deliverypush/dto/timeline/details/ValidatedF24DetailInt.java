package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class ValidatedF24DetailInt implements TimelineElementDetailsInt{
    private String status;

    public String toLog() {
        return String.format(
                "status=%s ",
                status
        );
    }
}