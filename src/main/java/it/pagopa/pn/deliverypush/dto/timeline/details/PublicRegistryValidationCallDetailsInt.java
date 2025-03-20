package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

import java.time.Instant;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class PublicRegistryValidationCallDetailsInt implements TimelineElementDetailsInt{

    private List<Integer> recIndexes;
    private DeliveryModeInt deliveryMode;
    private Instant sendDate;

    @Override
    public String toLog() {
        return String.format(
                "recIndexes=%s deliveryMode=%s sendDate=%s",
                recIndexes.toString(),
                deliveryMode,
                sendDate
        );
    }
}
