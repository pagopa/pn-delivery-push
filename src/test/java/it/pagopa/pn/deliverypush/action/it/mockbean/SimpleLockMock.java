package it.pagopa.pn.deliverypush.action.it.mockbean;

import net.javacrumbs.shedlock.core.SimpleLock;

import java.time.Duration;
import java.util.Optional;

public class SimpleLockMock implements SimpleLock {
    @Override
    public void unlock() {

    }

    @Override
    public Optional<SimpleLock> extend(Duration lockAtMostFor, Duration lockAtLeastFor) {
        return SimpleLock.super.extend(lockAtMostFor, lockAtLeastFor);
    }
}
