package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
    private Integer paFee;
    private NotificationSenderInt sender ;
    private List<NotificationRecipientInt> recipients ;
    private List<NotificationDocumentInt> documents ;
    private ServiceLevelTypeInt physicalCommunicationType;
    private Integer amount;
    private NotificationFeePolicy notificationFeePolicy;
    private Instant paymentExpirationDate;
    private PagoPaIntMode pagoPaIntMode;
    private String group;
}
