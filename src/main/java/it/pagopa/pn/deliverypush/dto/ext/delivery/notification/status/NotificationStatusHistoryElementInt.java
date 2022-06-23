package it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status;

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
public class NotificationStatusHistoryElementInt {
    private NotificationStatusInt status;
    private Instant activeFrom;
    private List<String> relatedTimelineElements;
}
