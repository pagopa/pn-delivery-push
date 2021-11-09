package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.deliverypush.webhook.cassandra.CassandraWebhookConfigEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

public interface WebhookConfigsDao {

    void setWebhook( String paId, String url );

    void setWebhookRun( String paId, boolean active );

    void setWebhookStartFrom(String paId, Instant startFrom);

    Optional<WebhookInfoDto> getWebhookInfo(String paId );

    Stream<WebhookInfoDto> activeWebhooks();

    void put(CassandraWebhookConfigEntity entity);
}
