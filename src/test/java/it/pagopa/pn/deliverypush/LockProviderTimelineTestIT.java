package it.pagopa.pn.deliverypush;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;


@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "pn.delivery-push.last-poll-for-future-action.lock-table-name" + "=" + "PnDeliveryPushShedLock"
})
@SpringBootTest
@Import(LocalStackTestConfig.class)
class LockProviderTimelineTestIT {
    @Autowired
    private LockProvider lockProvider;

    @Test
    void validateLockAfterWaiting() throws InterruptedException {
        String iun = "iun";
        Duration lockDuration = Duration.ofSeconds(8);
        // Take the lock
        Instant startAcquiredAt = Instant.now();
        lockProvider.lock(new LockConfiguration(startAcquiredAt, iun, lockDuration, Duration.ZERO));

        if(shouldCheckPrematureLockTest(startAcquiredAt, lockDuration)) {
            // Check if the lock is still held by trying to acquire it again (should fail)
            Optional<SimpleLock> prematureLock = lockProvider.lock(new LockConfiguration(Instant.now(), iun, lockDuration, Duration.ZERO));
            Assertions.assertFalse(prematureLock.isPresent(), "Lock should not be re-acquirable before unlock");
        }

        // Calculate time to wait for the lock to expire
        long remainingTimeToExceedLock = calculateRemainingTimeToExceedLock(startAcquiredAt, lockDuration);
        if(remainingTimeToExceedLock > 0) {
            Thread.sleep(remainingTimeToExceedLock);
        }

        // Check if the lock is still held by trying to acquire it again (should succeed)
        Optional<SimpleLock> optLockAfterWaiting = lockProvider.lock(new LockConfiguration(Instant.now(), iun, lockDuration, Duration.ZERO));
        Assertions.assertTrue(optLockAfterWaiting.isPresent(), "Lock should be re-acquirable after unlock");

        // Release the lock
        optLockAfterWaiting.ifPresent(SimpleLock::unlock);
    }

    @Test
    void validateLockRelease() {
        String iun = "iun";
        Duration lockDuration = Duration.ofSeconds(8);
        Instant startAcquiredAt = Instant.now();
        // Take the lock
        Optional<SimpleLock> firstLock = lockProvider.lock(new LockConfiguration(startAcquiredAt, iun, lockDuration, Duration.ZERO));

        if(shouldCheckPrematureLockTest(startAcquiredAt, lockDuration)) {
            // Check if the lock is still held by trying to acquire it again (should fail)
            Optional<SimpleLock> prematureLock = lockProvider.lock(new LockConfiguration(Instant.now(), iun, lockDuration, Duration.ZERO));
            Assertions.assertFalse(prematureLock.isPresent(), "Lock should not be re-acquirable before unlock");
        }

        // Release the lock
        firstLock.ifPresent(SimpleLock::unlock);

        // Check if the lock is still held by trying to acquire it again (should succeed)
        Optional<SimpleLock> optLockAfterWaiting = lockProvider.lock(new LockConfiguration(Instant.now(), iun, lockDuration, Duration.ZERO));
        Assertions.assertTrue(optLockAfterWaiting.isPresent(), "Lock should be re-acquirable after unlock");

        // Release the lock
        optLockAfterWaiting.ifPresent(SimpleLock::unlock);
    }

    /**
     * Calculate the remaining time to exceed the lock duration.
     *
     * <p>
     *     It is useful to spare time, if the previous lock process takes more time
     * </p>
     * @param startAcquiredAt Instant when the lock was acquired
     * @param lockDuration Duration of the lock
     * @return the remaining time to exceed the lock duration
     */
    private static long calculateRemainingTimeToExceedLock(Instant startAcquiredAt, Duration lockDuration) {
        Instant midTime = Instant.now();
        Duration actualLockDuration = Duration.between(startAcquiredAt, midTime);
        // Slightly more than lockAtMostFor
        return lockDuration.toMillis() - actualLockDuration.toMillis() + 1000;
    }

    /**
     * The method checks if the lock duration is already exceeded, in that case it skips the premature lock test.
     * <p>
     * It is required because in CI env sometimes the lock acquire process takes more than it should and the lock is already expired
     * when it comes to this assertion, so we can skip it.
     * </p>
     * @param startAcquiredAt Instant when the lock was acquired
     * @param lockDuration Duration of the lock
     * @return true if the lock duration is not exceeded yet, false otherwise
     */
    private boolean shouldCheckPrematureLockTest(Instant startAcquiredAt, Duration lockDuration) {
        Instant endAcquiredAt = Instant.now();
        Duration actualLockDuration = Duration.between(startAcquiredAt, endAcquiredAt);
        if (actualLockDuration.compareTo(lockDuration) >= 0) {
            System.out.println("Lock duration is already exceeded, skipping premature lock test");
            return false;
        }
        return true;
    }

}