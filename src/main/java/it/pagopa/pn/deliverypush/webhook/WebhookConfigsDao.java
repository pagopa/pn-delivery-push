package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.api.dto.webhook.WebhookConfigDto;
import it.pagopa.pn.deliverypush.webhook.cassandra.CassandraWebhookConfigEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

public interface WebhookConfigsDao {

    void setWebhook( String paId, String url );

    void setWebhookRun( String paId, boolean active );

    void setWebhookStartFrom(String paId, Instant startFrom);

    Optional<WebhookConfigDto> getWebhookInfo(String paId);

    Stream<WebhookConfigDto> activeWebhooks();

    void put(CassandraWebhookConfigEntity entity);
}
