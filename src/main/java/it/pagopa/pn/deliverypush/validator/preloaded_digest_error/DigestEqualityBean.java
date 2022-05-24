package it.pagopa.pn.deliverypush.validator.preloaded_digest_error;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@ToString
@DigestEquality
public class DigestEqualityBean {
//TODO Da eliminare, una volta implementata la PN-764

    private String key;
    private NotificationDocumentInt.Digests expected;
    private NotificationDocumentInt.Digests actual;
}
