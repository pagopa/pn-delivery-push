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

    private String key;
    private NotificationAttachment.Digests expected;
    private NotificationAttachment.Digests actual;
}
