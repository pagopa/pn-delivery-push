package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.commons.abstractions.impl.AbstractCassandraKeyValueStore;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Component;

@Component
public class CassandraWebhookBufferEntityDao extends AbstractCassandraKeyValueStore<WebhookBufferRowEntityId, WebhookBufferRowEntity> {

    public CassandraWebhookBufferEntityDao(CassandraOperations cassandraTemplate) {
        super(cassandraTemplate, WebhookBufferRowEntity.class);
    }
}
