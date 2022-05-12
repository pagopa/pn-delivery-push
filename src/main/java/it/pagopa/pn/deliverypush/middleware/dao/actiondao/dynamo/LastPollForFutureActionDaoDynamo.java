package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.LastPollForFutureActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.LastPollForFutureActionsDao;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.Optional;

@Component
public class LastPollForFutureActionDaoDynamo implements LastPollForFutureActionsDao {
    public static final long LAST_POLL_KEY = 1L;

    private final LastPollForFutureActionEntityDao entityDao;

    public LastPollForFutureActionDaoDynamo(LastPollForFutureActionEntityDao entityDao) {
        this.entityDao = entityDao;
    }

    @Override
    public void updateLastPollTime(Instant lastPollExecuted) {
        entityDao.put(LastPollForFutureActionEntity.builder()
                .lastPollKey(LAST_POLL_KEY)
                .lastPollExecuted( lastPollExecuted )
                .build());
    }

    @Override
    public Optional<Instant> getLastPollTime() {
        Key keyToSearch = Key.builder()
                .partitionValue(LAST_POLL_KEY)
                .build();

        Optional<LastPollForFutureActionEntity> entity = entityDao.get(keyToSearch);

        return ( entity.isEmpty() )
                ? Optional.empty()
                : Optional.ofNullable( entity.get().getLastPollExecuted());
    }
}
