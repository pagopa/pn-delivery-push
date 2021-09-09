package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TimeParams.class)
@EnableConfigurationProperties(value = PnDeliveryPushConfigs.class)
@TestPropertySource(properties = {"pn.delivery-push.time-params.second-attempt-waiting-time=1s",
        "pn.delivery-push.time-params.recipient-view-max-time=1s",
        "pn.delivery-push.time-params.waiting-response-from-first-address=1s",
        "pn.delivery-push.time-params.waiting-for-next-action=1m",
        "pn.delivery-push.time-params.time-between-ext-ch-reception-and-message-processed=1h",
        "pn.delivery-push.time-params.interval-between-notification-and-message-received=1d"})
class TimeParamsTest {

    @Autowired
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    @Test
    void assertConfigurationIsLoaded(){
        
        assertEquals( Duration.ofSeconds(1), pnDeliveryPushConfigs.getTimeParams().getSecondAttemptWaitingTime() );
        assertEquals( Duration.ofSeconds(1), pnDeliveryPushConfigs.getTimeParams().getRecipientViewMaxTime() );
        assertEquals( Duration.ofSeconds(1), pnDeliveryPushConfigs.getTimeParams().getWaitingResponseFromFirstAddress() );

    }

    @Test
    void assertConfigurationIsLoadedOfMinutes(){

        assertEquals( Duration.ofMinutes(1), pnDeliveryPushConfigs.getTimeParams().getWaitingForNextAction() );

    }
    @Test
    void assertConfigurationIsLoadedOfHours(){

        assertEquals( Duration.ofHours(1), pnDeliveryPushConfigs.getTimeParams().getTimeBetweenExtChReceptionAndMessageProcessed() );

    }
    @Test
    void assertConfigurationIsLoadedOfDays(){

        assertEquals( Duration.ofDays(1), pnDeliveryPushConfigs.getTimeParams().getIntervalBetweenNotificationAndMessageReceived() );

    }

}
