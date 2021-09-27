package it.pagopa.pn.deliverypush.webhook.cassandra;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import it.pagopa.pn.deliverypush.webhook.WebhookConfigsDao;
import it.pagopa.pn.deliverypush.webhook.WebhookInfoDto;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.cql.QueryOptions;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.data.cassandra.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class CassandraWebhookConfigEntityDao implements WebhookConfigsDao {

    public static final QueryOptions QUERY_OPTIONS = QueryOptions.builder().consistencyLevel(ConsistencyLevel.LOCAL_QUORUM).build();

    private final CassandraOperations cassandra;

    public CassandraWebhookConfigEntityDao(CassandraOperations cassandra) {
        this.cassandra = cassandra;
    }

    @Override
    public void setWebhook(String paId, String url) {
        updateFieldByPaId(paId, "url", url);
    }

    @Override
    public void setWebhookRun( String paId, boolean active ) {
        updateFieldByPaId(paId, "active", active );
    }

    @Override
    public void setWebhookStartFrom(String paId, Instant startFrom) {
        updateFieldByPaId(paId, "since", startFrom );
    }

    @Override
    public Optional<WebhookInfoDto> getWebhookInfo(String paId) {
        CassandraWebhookConfigEntity entity = this.cassandra.selectOneById( paId, CassandraWebhookConfigEntity.class );
        return Optional.ofNullable( entity ).map( this::entity2dto );
    }

    @Override
    public Stream<WebhookInfoDto> activeWebhooks() {
        String tableName = this.cassandra.getTableName( CassandraWebhookConfigEntity.class ).asCql(true);
        return this.cassandra.stream("SELECT * FROM " + tableName, CassandraWebhookConfigEntity.class)
                .filter( CassandraWebhookConfigEntity::isActive )
                .map( this::entity2dto );
    }

    private WebhookInfoDto entity2dto(CassandraWebhookConfigEntity entity) {
        return WebhookInfoDto.builder()
                .paId(entity.getPaId())
                .url(entity.getUrl())
                .active(entity.isActive())
                .startFrom(entity.getSince())
                .build();
    }

    private void updateFieldByPaId(String paId, String fieldName, Object value) {
        Query query = Query.query( Criteria.where("paId").is(paId) )
                .queryOptions(QUERY_OPTIONS);
        Update update = Update.update(fieldName, value);
        this.cassandra.update( query, update, CassandraWebhookConfigEntity.class );
    }

}
