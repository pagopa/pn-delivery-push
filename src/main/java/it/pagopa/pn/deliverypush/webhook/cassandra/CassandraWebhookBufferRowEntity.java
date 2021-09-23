package it.pagopa.pn.deliverypush.webhook.cassandra;

import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("webhook_buffer")
@Getter
@Builder( toBuilder = true )
public class CassandraWebhookBufferRowEntity {

    @PrimaryKey
    private CassandraWebhookBufferRowEntityId id;
    private NotificationStatus status;
    
}
