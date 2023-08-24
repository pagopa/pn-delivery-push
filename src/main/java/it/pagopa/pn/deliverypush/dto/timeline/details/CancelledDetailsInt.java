package it.pagopa.pn.deliverypush.dto.timeline.details;

import java.util.Arrays;
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
public class CancelledDetailsInt implements RecipientRelatedTimelineElementDetails {

    private int recIndex;
    private int notificationCost;
    private int[] notRefinedRecipientIndexes;

    public String toLog() {
        return String.format(
            "notificationCost=%d notRefinedRecipientIndexes=%s",
            notificationCost,
            Arrays.toString(notRefinedRecipientIndexes)
        );
    }
}