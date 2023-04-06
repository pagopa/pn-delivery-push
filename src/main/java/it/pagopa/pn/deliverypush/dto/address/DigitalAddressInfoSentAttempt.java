package it.pagopa.pn.deliverypush.dto.address;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SuperBuilder( toBuilder = true )
@EqualsAndHashCode( callSuper = true )
@ToString
public class DigitalAddressInfoSentAttempt  extends DigitalAddressInfo{
    private int sentAttemptMade;
    private Instant lastAttemptDate;
    private String relatedFeedbackTimelineId;
}
