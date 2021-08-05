package it.pagopa.pn.deliverypush;

import it.pagopa.pn.deliverypush.dao.NewNotificationEvtMOM;
import it.pagopa.pn.deliverypush.dao.PecRequestMOM;
import it.pagopa.pn.deliverypush.events.NewNotificationEvt;
import it.pagopa.pn.deliverypush.events.PecRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

@Service
public class FakeWorkflowService {

    private Logger logger = LoggerFactory.getLogger( this.getClass() );

    private final PecRequestMOM pecMom;
    private final NewNotificationEvtMOM notificationMom;

    public FakeWorkflowService(PecRequestMOM pecMom, NewNotificationEvtMOM notificationMom) {
        this.pecMom = pecMom;
        this.notificationMom = notificationMom;
    }

    @Scheduled(fixedDelay = 1000)
    public void scheduleFixedDelayTask() {
        //logger.info("Method Scheduled" );

        try {
            notificationMom.poll( Duration.ofSeconds(5)).thenApply( (notifications) -> {
                //logger.info("Queue polling done" );
                notifications.forEach( n -> {
                    logger.info("Received IUN " + n.getIun() + "! Ciao mondo!" );
                    pecMom.push(
                            PecRequest.builder()
                                    .iun( n.getIun())
                                    .address("hello@world.it")
                                    .sentDate( n.getSentDate())
                                .build()
                        );
                });
                return null;
            }).get();
        } catch (InterruptedException | ExecutionException exc) {
            exc.printStackTrace( System.err );
        }

    }
}
