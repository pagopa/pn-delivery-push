package it.pagopa.pn.deliverypush.validator.preloaded_digest_error;

import it.pagopa.pn.api.dto.notification.NotificationAttachment;
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
    private NotificationAttachment.Digests expected;
    private NotificationAttachment.Digests actual;
}
