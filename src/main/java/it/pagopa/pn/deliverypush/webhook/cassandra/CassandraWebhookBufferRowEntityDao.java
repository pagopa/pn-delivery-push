package it.pagopa.pn.deliverypush.webhook.cassandra;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.deliverypush.webhook.WebhookBufferDao;
import it.pagopa.pn.deliverypush.webhook.WebhookBufferRowDto;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.data.cassandra.core.cql.QueryOptions;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.stream.Stream;

@Component
public class CassandraWebhookBufferRowEntityDao implements WebhookBufferDao {

    public static final QueryOptions QUERY_OPTIONS = QueryOptions.builder().consistencyLevel(ConsistencyLevel.LOCAL_QUORUM).build();
    public static final InsertOptions INSERT_OPTIONS = InsertOptions.builder().consistencyLevel(ConsistencyLevel.LOCAL_QUORUM).build();

    private final CassandraOperations cassandra;

    public CassandraWebhookBufferRowEntityDao(CassandraOperations cassandra) {
        this.cassandra = cassandra;
    }


    @Override
    public void put(String senderId, String iun, Instant statusChangeDate, NotificationStatus newStatus) {
        CassandraWebhookBufferRowEntity entity = CassandraWebhookBufferRowEntity.builder()
                .id( CassandraWebhookBufferRowEntityId.builder()
                        .senderId( senderId )
                        .iun( iun )
                        .statusChangeTime( statusChangeDate )
                        .build()
                )
                .status( newStatus )
                .build();

        cassandra.insert( entity, INSERT_OPTIONS );
    }

    @Override
    public Stream<WebhookBufferRowDto> bySenderIdAndDate(String senderId, Instant notBefore) {
        Query query = Query.query(
                Criteria.where("id.senderId").is( senderId ),
                Criteria.where("id.statusChangeTime").gt( notBefore ),
                Criteria.where("id.statusChangeTime").lte( Instant.now() )
            )
            .queryOptions( QUERY_OPTIONS );

        return this.cassandra.stream(query, CassandraWebhookBufferRowEntity.class)
                .map( this::entity2dto );
    }

    private WebhookBufferRowDto entity2dto(CassandraWebhookBufferRowEntity entity) {
        return WebhookBufferRowDto.builder()
                .senderId( entity.getId().getSenderId() )
                .iun( entity.getId().getIun() )
                .statusChangeTime( entity.getId().getStatusChangeTime() )
                .status( entity.getStatus() )
                .build();
    }
}
