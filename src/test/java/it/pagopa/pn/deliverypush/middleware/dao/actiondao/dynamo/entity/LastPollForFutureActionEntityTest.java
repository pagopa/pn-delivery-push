package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class LastPollForFutureActionEntityTest {

    private LastPollForFutureActionEntity entity;

    @BeforeEach
    public void setup() {
        entity = new LastPollForFutureActionEntity();
    }

    @Test
    void getLastPollKey() {
        entity.setLastPollKey(1L);
        Assertions.assertEquals(1L, entity.getLastPollKey());

    }

    @Test
    void getLastPollExecuted() {
        Instant in = Instant.parse("2021-09-16T15:24:00.00Z");
        entity.setLastPollExecuted(in);
        Assertions.assertEquals(in, entity.getLastPollExecuted());
    }
}