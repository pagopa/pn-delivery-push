package it.pagopa.pn.deliverypush.middleware.momconsumer;

import java.time.Duration;

public interface EventReceiver {
    void poll(Duration maxPollTime);
}
