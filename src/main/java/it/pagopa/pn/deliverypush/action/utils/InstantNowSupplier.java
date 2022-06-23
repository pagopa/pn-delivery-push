package it.pagopa.pn.deliverypush.action.utils;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class InstantNowSupplier {

    public Instant get() {
        return Instant.now();
    }
}
