package it.pagopa.pn.deliverypush.dto.timeline.details;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@ToString
public class NotificationCancelledDetailsInt extends CategoryTypeTimelineElementDetailsInt implements TimelineElementDetailsInt {

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