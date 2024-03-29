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
public class SendInformation extends DigitalAddressInfo{
    private int retryNumber;
    private Instant eventTimestamp;
    private Boolean isFirstSendRetry;
    private String relatedFeedbackTimelineId;
}
