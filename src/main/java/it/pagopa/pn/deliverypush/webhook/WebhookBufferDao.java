package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.api.dto.notification.status.NotificationStatus;

import java.time.Instant;
import java.util.stream.Stream;

public interface WebhookBufferDao {

    void put(String senderId, String iun, Instant statusChangeDate, NotificationStatus newStatus);

    Stream<WebhookBufferRowDto> bySenderIdAndDate(String senderId, Instant notBefore );

}
