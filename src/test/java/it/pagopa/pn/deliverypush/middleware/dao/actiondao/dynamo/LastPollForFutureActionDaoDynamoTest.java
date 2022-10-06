package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.LastPollForFutureActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.LastPollForFutureActionEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.Optional;

class LastPollForFutureActionDaoDynamoTest {

    public static final long LAST_POLL_KEY = 1L;

    @Mock
    private LastPollForFutureActionEntityDao entityDao;

    private LastPollForFutureActionDaoDynamo dynamo;

    @BeforeEach
    public void setup() {

        entityDao = Mockito.mock(LastPollForFutureActionEntityDao.class);
        dynamo = new LastPollForFutureActionDaoDynamo(entityDao);
    }

    @Test
    void updateLastPollTime() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");
        LastPollForFutureActionEntity entity = buildLastPollForFutureActionEntity();
        dynamo.updateLastPollTime(instant);

        Mockito.verify(entityDao, Mockito.times(1)).put(entity);
    }

    @Test
    void getLastPollTime() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");
        Key keyToSearch = Key.builder()
                .partitionValue(LAST_POLL_KEY)
                .build();

        LastPollForFutureActionEntity entity = buildLastPollForFutureActionEntity();

        Mockito.when(entityDao.get(keyToSearch)).thenReturn(Optional.of(entity));

        Optional<Instant> actual = dynamo.getLastPollTime();

        Assertions.assertEquals(instant, actual.get());
    }

    private LastPollForFutureActionEntity buildLastPollForFutureActionEntity() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");

        return LastPollForFutureActionEntity.builder()
                .lastPollKey(LAST_POLL_KEY)
                .lastPollExecuted(instant)
                .build();
    }
}