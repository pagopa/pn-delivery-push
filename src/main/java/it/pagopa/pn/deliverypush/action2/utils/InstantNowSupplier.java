package it.pagopa.pn.deliverypush.action2.utils;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.function.Supplier;

@Component
public class InstantNowSupplier {

    public Instant get() {
        return Instant.now();
    }
}
