package it.pagopa.pn.deliverypush.temp.mom.consumer;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class EventReceiverService {

    private final List<EventReceiver> eventsRecevers;

    public EventReceiverService(List<EventReceiver> eventsRecevers) {
        this.eventsRecevers = eventsRecevers;
    }

//    @Scheduled( fixedDelay = 100 )
    public void doPoll() {
        for( EventReceiver evtRec : eventsRecevers ) {
            evtRec.poll( Duration.ofSeconds( 1 ));
        }
    }
}
