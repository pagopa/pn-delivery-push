package it.pagopa.pn.deliverypush.action.it.mockbean;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import java.util.Optional;

public class LockProviderMock implements LockProvider {

    @Override
    public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
        return Optional.of(new SimpleLockMock());
    }
}
