package it.pagopa.pn.deliverypush.middleware.dao.timelineshedlockdao.dynamo;

import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;


@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "pn.delivery-push.timeline-shedlock-dao.table-name" + "=" + "TimelinesShedlock"
})
@SpringBootTest
@Import(LocalStackTestConfig.class)
class LockProviderTimelineTestIT {
    @Qualifier("lockProviderTimeline")
    @Autowired
    private LockProvider lockProvider;
    @Autowired
    private TimelineShedlockEntityDaoDynamo dao;

    @Test
    void validateLock() {
        String iun = "iun";
        Instant firstLockInstant = Instant.now();
        Duration lockDuration = Duration.ofSeconds(30);
        Instant expectedFirstLockUntil = firstLockInstant.plus(lockDuration);

        // Take the lock
        Optional<SimpleLock> optLock = takeLock(iun, firstLockInstant, lockDuration);
        verifyLock(iun, expectedFirstLockUntil, optLock);

        // Try to take the lock again
        Optional<SimpleLock> optLock2 = takeLock(iun, Instant.now(), lockDuration);
        // Verify that the lock is already taken
        Assertions.assertFalse(optLock2.isPresent());


        // Release the first lock
        optLock.get().unlock();
        // Try to take the lock again
        Instant thirdLockInstant = Instant.now();
        Instant expectedThirdLockUntil = thirdLockInstant.plus(lockDuration);
        Optional<SimpleLock> optLock3 = takeLock(iun, thirdLockInstant, lockDuration);
        verifyLock(iun, expectedThirdLockUntil, optLock3);
    }

    private Optional<SimpleLock> takeLock(String iun, Instant now, Duration lockDuration) {
        return lockProvider.lock(new LockConfiguration(now, iun, lockDuration, Duration.ZERO));
    }

    private void verifyLock(String iun, Instant expectedLockUntil, Optional<SimpleLock> optLock) {
        TimelineShedlockEntity entity = dao.getItem(iun);
        Assertions.assertTrue(optLock.isPresent());
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(iun, entity.getIun());
        Assertions.assertEquals(expectedLockUntil.truncatedTo(ChronoUnit.MILLIS), entity.getLockUntil().truncatedTo(ChronoUnit.MILLIS));
    }
}