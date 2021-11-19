package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.deliverypush.webhook.dto.WebhookBufferRowDto;

import java.time.Instant;
import java.util.stream.Stream;

public interface WebhookBufferDao {

    void put(String senderId, String iun, Instant statusChangeDate, String notificationElement);

    Stream<WebhookBufferRowDto> bySenderIdAndDate(String senderId, Instant notBefore );

}
