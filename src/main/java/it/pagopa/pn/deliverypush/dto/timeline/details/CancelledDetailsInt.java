package it.pagopa.pn.deliverypush.dto.timeline.details;

import java.util.List;
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
public class CancelledDetailsInt implements TimelineElementDetailsInt {

    private int notificationCost;
    private List<Integer> notRefinedRecipientIndexes;

    public String toLog() {
        return String.format(
            "notificationCost=%d notRefinedRecipientIndexes=%s",
            notificationCost,
            notRefinedRecipientIndexes.toString()
        );
    }
}