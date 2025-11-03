package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElementV26;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
public class NotificationTimelineReworkedDetails implements RecipientRelatedTimelineElementDetails, TimelineElementDetailsInt {
    private int recIndex;
    private Integer sentAttemptMade;
    private List<NotificationStatusHistoryElementV26> invalidatedTimelineAndStatusHistory;
    private String reason;
    private String categoryType;

    @Override
    public String toLog() {
        return String.format(
                "NotificationTimelineReworkedDetails{recIndex=%d, sentAttemptMade=%d, reason='%s', categoryType='%s', invalidatedTimelineAndStatusHistory=%s}",
                recIndex,
                sentAttemptMade,
                reason,
                categoryType,
                invalidatedTimelineAndStatusHistory
        );
    }

}
