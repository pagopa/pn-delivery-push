package it.pagopa.pn.deliverypush.middleware.actiondao.cassandra;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.LastPollForFutureActionsDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@Slf4j
public class CassandraLastPollForFutureActionsDao implements LastPollForFutureActionsDao {

    private static final InsertOptions INSERT_OPTIONS = InsertOptions.builder()
            .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
            .build();
    public static final long LAST_POLL_KEY = 1L;

    private final CassandraOperations cassandra;

    public CassandraLastPollForFutureActionsDao(CassandraOperations cassandra ) {
        this.cassandra = cassandra;
    }

    @Override
    public void updateLastPollTime(Instant lastPollExecuted) {
        cassandra.insert( LastPollForFutureActionsEntity.builder()
                .lastPollKey(LAST_POLL_KEY)
                .lastPollExecuted( lastPollExecuted )
                .build(),
                INSERT_OPTIONS
            );
    }

    @Override
    public Optional<Instant> getLastPollTime() {
        LastPollForFutureActionsEntity entity = cassandra.selectOneById(LAST_POLL_KEY, LastPollForFutureActionsEntity.class);
        return ( entity == null )
                ? Optional.empty()
                : Optional.ofNullable( entity.getLastPollExecuted() );
    }


}
