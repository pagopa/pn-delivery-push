package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class RequestRefusedDetailsInt implements TimelineElementDetailsInt {
    private List<String> errors;

    public String toLog() {
        return String.format(
                "errors=%s",
                errors
        );
    }
}
