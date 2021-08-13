package it.pagopa.pn.deliverypush.middleware;

import java.time.Duration;

public interface EventReceiver {
    void poll(Duration maxPollTime);
}
