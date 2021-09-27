package it.pagopa.pn.deliverypush.webhook.cassandra;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.time.Instant;

@Builder( toBuilder = true )
@Getter
@PrimaryKeyClass
@EqualsAndHashCode
public class CassandraWebhookBufferRowEntityId implements Serializable {

    @PrimaryKeyColumn(name = "senderId", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String senderId;

    @PrimaryKeyColumn(name = "statusChangeTime", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private Instant statusChangeTime;

    @PrimaryKeyColumn(name = "iun", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private String iun;

}
