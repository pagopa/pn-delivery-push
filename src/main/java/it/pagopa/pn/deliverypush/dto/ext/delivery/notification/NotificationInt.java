package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;


import lombok.*;

import java.time.Instant;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationInt {
    private String iun;
    private String paProtocolNumber;
    private String subject;
    private Instant sentAt;
    private NotificationSenderInt sender ;
    private List<NotificationRecipientInt> recipients ;
    private List<NotificationDocumentInt> documents ;
    private ServiceLevelTypeInt physicalCommunicationType;
}
