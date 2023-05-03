package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class CompletelyUnreachableCreationRequestDetails implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private String legalFactId;
    private Instant completionWorkflowDate;
    private EndWorkflowStatus endWorkflowStatus;

    public String toLog() {
        return String.format(
                "recIndex=%d endWorkflowStatus%s completionWorkflowDate=%s legalFactId=%s",
                recIndex,
                endWorkflowStatus,
                completionWorkflowDate,
                legalFactId
        );
    }
}
