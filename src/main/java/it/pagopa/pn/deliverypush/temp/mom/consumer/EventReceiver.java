package it.pagopa.pn.deliverypush.temp.mom.consumer;

import java.time.Duration;

public interface EventReceiver {
    void poll(Duration maxPollTime);
}
