package it.pagopa.pn.deliverypush.middleware.dao.actiondao;



import java.time.Instant;
import java.util.Optional;

public interface LastPollForFutureActionsDao {

    void updateLastPollTime(Instant lastPollExecuted);

    Optional<Instant> getLastPollTime();
    
}
