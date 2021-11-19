package it.pagopa.pn.deliverypush.webhook.cassandra;

import it.pagopa.pn.api.dto.webhook.WebhookType;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.Set;

@Table("webhook_configs")
@Getter
@Builder(toBuilder = true)
public class CassandraWebhookConfigEntity {

    @PrimaryKey
    private String paId;

    private String url;
    private Instant since;
    private boolean active;
    private WebhookType type;
    private boolean allNotifications;
    private Set<String> notificationsElement;

}
