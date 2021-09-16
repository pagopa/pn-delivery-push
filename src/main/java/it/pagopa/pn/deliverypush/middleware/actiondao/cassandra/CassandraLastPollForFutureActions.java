package it.pagopa.pn.deliverypush.middleware.actiondao.cassandra;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.pn.deliverypush.abstractions.actionspool.LastPollForFutureActions;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.LastPollForFutureActionsDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class CassandraLastPollForFutureActions implements LastPollForFutureActionsDao {

    private static final InsertOptions INSERT_OPTIONS = InsertOptions.builder()
            .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
            .build();

    private final CassandraOperations cassandra;
    private final ObjectWriter lastPollForFutureActionsWriter;
    private final ObjectReader lastPollForFutureActionsReader;

    public CassandraLastPollForFutureActions(CassandraOperations cassandra, ObjectMapper objMapper) {
        this.cassandra = cassandra;
        this.lastPollForFutureActionsWriter = objMapper.writerFor(LastPollForFutureActions.class);
        this.lastPollForFutureActionsReader = objMapper.readerFor(LastPollForFutureActions.class);
    }

    @Override
    public void updateLastPollForFutureActions(LastPollForFutureActions lastPollForFutureActions) {
        cassandra.insert(dto2Entity(lastPollForFutureActions));
    }

    @Override
    public Optional<LastPollForFutureActions> getLastPollForFutureActionsById() {
        LastPollForFutureActionsEntity entity = cassandra.selectOneById(1L, LastPollForFutureActionsEntity.class);
        return Optional.ofNullable(entity)
                .map(en -> entity2dto(en));
    }

    public LastPollForFutureActionsEntity dto2Entity (LastPollForFutureActions dto){
        LastPollForFutureActionsEntity.LastPollForFutureActionsEntityBuilder builder = LastPollForFutureActionsEntity.builder()
                .lastPollExecuted(dto.getLastPollExecuted())
                .lastPollKey(1L);

        return builder.build();
    }

    public LastPollForFutureActions entity2dto(LastPollForFutureActionsEntity entity){
        LastPollForFutureActions.LastPollForFutureActionsBuilder builder = LastPollForFutureActions.builder()
                .lastPollExecuted(entity.getLastPollExecuted());

        return builder.build();
    }

}
