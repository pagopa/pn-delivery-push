package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;



import java.time.Instant;
import java.util.Optional;

public interface LastPollForFutureActionsDao {

    void updateLastPollTime(Instant lastPollExecuted);

    Optional<Instant> getLastPollTime();



}
