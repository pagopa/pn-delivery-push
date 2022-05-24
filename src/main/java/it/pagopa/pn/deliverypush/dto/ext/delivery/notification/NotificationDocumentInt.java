package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.constraints.CheckSha256;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@CheckSha256
public class NotificationDocumentInt {
    private Digests digests;
    private Ref ref;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Builder(toBuilder = true)
    @EqualsAndHashCode
    @ToString
    public static class Digests {
        private String sha256;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Builder(toBuilder = true)
    @EqualsAndHashCode
    @ToString
    public static class Ref {
        private String key;
        private String versionToken;
    }
}
