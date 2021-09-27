package it.pagopa.pn.deliverypush.webhook.cassandra;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Table("webhook_configs")
@Getter
@Builder( toBuilder = true )
public class CassandraWebhookConfigEntity {

    @PrimaryKey
    private String paId;

    private String url;
    private Instant since;
    private boolean active;

}
