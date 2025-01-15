package it.pagopa.pn.deliverypush;

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

    @Test
    void validateLockAfterWaiting() throws InterruptedException {
        String iun = "iun";
        Duration lockDuration = Duration.ofSeconds(3);
        // Take the lock
        lockProvider.lock(new LockConfiguration(Instant.now(), iun, lockDuration, Duration.ZERO));

        // Check if the lock is still held by trying to acquire it again (should fail)
        Optional<SimpleLock> prematureLock = lockProvider.lock(new LockConfiguration(Instant.now(), iun, lockDuration, Duration.ZERO));
        Assertions.assertFalse(prematureLock.isPresent(), "Lock should not be re-acquirable before unlock");

        // Wait for the lock to expire
        Thread.sleep(3000);

        // Check if the lock is still held by trying to acquire it again (should succeed)
        Optional<SimpleLock> optLockAfterWaiting = lockProvider.lock(new LockConfiguration(Instant.now(), iun, lockDuration, Duration.ZERO));
        Assertions.assertTrue(optLockAfterWaiting.isPresent(), "Lock should be re-acquirable after unlock");

        // Release the lock
        optLockAfterWaiting.ifPresent(SimpleLock::unlock);
    }

    @Test
    void validateLockRelease() {
        String iun = "iun";
        Duration lockDuration = Duration.ofSeconds(3);
        // Take the lock
        Optional<SimpleLock> firstLock = lockProvider.lock(new LockConfiguration(Instant.now(), iun, lockDuration, Duration.ZERO));

        // Check if the lock is still held by trying to acquire it again (should fail)
        Optional<SimpleLock> prematureLock = lockProvider.lock(new LockConfiguration(Instant.now(), iun, lockDuration, Duration.ZERO));
        Assertions.assertFalse(prematureLock.isPresent(), "Lock should not be re-acquirable before unlock");

        // Release the lock
        firstLock.ifPresent(SimpleLock::unlock);

        // Check if the lock is still held by trying to acquire it again (should succeed)
        Optional<SimpleLock> optLockAfterWaiting = lockProvider.lock(new LockConfiguration(Instant.now(), iun, lockDuration, Duration.ZERO));
        Assertions.assertTrue(optLockAfterWaiting.isPresent(), "Lock should be re-acquirable after unlock");

        // Release the lock
        optLockAfterWaiting.ifPresent(SimpleLock::unlock);
    }

}