package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.DigitalMessageReference;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.LegalMessageSentDetails;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.ProgressEventCategory;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalDetailsInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.middleware.responsehandler.ExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.await;

@Slf4j
public class ExternalChannelMock implements ExternalChannelSendClient {
    //DIGITAL
    public static final String EXT_CHANNEL_SEND_FAIL_BOTH = "fail-both";
    public static final String EXT_CHANNEL_SEND_FAIL_FIRST = "fail-first";
    public static final String EXT_CHANNEL_WORKS = "works";

    //ANALOG
    public static final String EXTCHANNEL_SEND_SUCCESS = "OK"; //Invio notifica ok
    public static final String EXTCHANNEL_SEND_FAIL = "FAIL"; //Invio notifica fallita
    public static final String EXT_CHANNEL_SEND_NEW_ADDR = "NEW_ADDR:"; //Invio notifica fallita con nuovo indirizzo da investigazione
    //Esempio: La combinazione di EXT_CHANNEL_SEND_NEW_ADDR + EXTCHANNEL_SEND_OK ad esempio significa -> Invio notifica fallito ma con nuovo indirizzo trovato e l'invio a tale indirzzo avrà successo

    public static final int WAITING_TIME = 100;
    private static final Pattern NEW_ADDRESS_INPUT_PATTERN = Pattern.compile("^" + EXT_CHANNEL_SEND_NEW_ADDR + "(.*)$");

    private final ExternalChannelResponseHandler externalChannelHandler;
    private final TimelineService timelineService;
    private final InstantNowSupplier instantNowSupplier;

    public ExternalChannelMock(@Lazy ExternalChannelResponseHandler externalChannelHandler,
                               @Lazy TimelineService timelineService,
                               @Lazy InstantNowSupplier instantNowSupplier) {
        this.externalChannelHandler = externalChannelHandler;
        this.timelineService = timelineService;
        this.instantNowSupplier = instantNowSupplier;
    }

    @Override
    public void sendLegalNotification(NotificationInt notificationInt,
                                      NotificationRecipientInt recipientInt,
                                      LegalDigitalAddressInt digitalAddress,
                                      String timelineEventId,
                                      String aarKey,
                                      String quickAccessToken) {
        //Invio messaggio legali necessità di risposta da external channel
        sendDigitalNotification(digitalAddress.getAddress(), notificationInt, timelineEventId);
    }


    public void sendCourtesyNotification(NotificationInt notificationInt,
                                         NotificationRecipientInt recipientInt,
                                         CourtesyDigitalAddressInt digitalAddress,
                                         String timelineEventId,
                                         String aarKey) {
        //Invio messaggio di cortesia non necessità di risposta da external channel
        //sendDigitalNotification(digitalAddress.getAddress(), notificationInt, timelineEventId, false);
    }

    private void sendDigitalNotification(String address, NotificationInt notification, String timelineEventId){
        log.info("sendDigitalNotification address:{} requestId:{}", address, timelineEventId);
        
        new Thread(() -> {
            Assertions.assertDoesNotThrow(() -> {
                // Viene atteso fino a che l'elemento di timeline relativo all'invio verso extChannel sia stato inserito
                await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                        Assertions.assertTrue(timelineService.getTimelineElement(notification.getIun(), timelineEventId).isPresent())
                );

                simulateExternalChannelDigitalProgressResponse(timelineEventId);

                Optional<SendDigitalDetailsInt> sendDigitalDetailsOpt = timelineService.getTimelineElementDetails(notification.getIun(), timelineEventId, SendDigitalDetailsInt.class);
                if(sendDigitalDetailsOpt.isPresent()){
                    waitForProgressTimelineElement(notification, sendDigitalDetailsOpt.get());

                    simulateExternalChannelDigitalResponse(address, timelineEventId);

                }else {
                    log.error("SendDigitalDetails is not present");
                }
            });
        }).start();
    }

    private void waitForProgressTimelineElement(NotificationInt notification, SendDigitalDetailsInt sendDigitalDetails) {

        String elementId = TimelineEventId.SEND_DIGITAL_PROGRESS.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(sendDigitalDetails.getRecIndex())
                        .sentAttemptMade(sendDigitalDetails.getRetryNumber())
                        .source(sendDigitalDetails.getDigitalAddressSource())
                        .index(0)
                        .progressIndex(1)
                        .build()
        );
        
        //Viene atteso finchè l'elemento di timeline relativo al progress non sia stato inserito
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(notification.getIun(), elementId).isPresent())
        );
    }

    private void simulateExternalChannelDigitalProgressResponse(String timelineEventId) {
        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        LegalMessageSentDetails extChannelResponse = new LegalMessageSentDetails();
        extChannelResponse.setStatus(ProgressEventCategory.PROGRESS);
        extChannelResponse.setEventTimestamp(Instant.now().atOffset(ZoneOffset.UTC));
        extChannelResponse.setRequestId(timelineEventId);
        extChannelResponse.setEventCode(LegalMessageSentDetails.EventCodeEnum.C001); //ACCETTAZIONE //TODO Da gestire la non accettazione
        extChannelResponse.setGeneratedMessage(
                new DigitalMessageReference()
                        .id("test_id")
                        .location("safestorage://urlditest")
                        .system("test_system")
        );

        singleStatusUpdate.setDigitalLegal(extChannelResponse);
        
        externalChannelHandler.extChannelResponseReceiver(singleStatusUpdate);
    }

    private void simulateExternalChannelDigitalResponse(String address, String timelineEventId) {

        ProgressEventCategory status;

        String eventId = timelineEventId;
        String retryNumberPart = eventId.replaceFirst(".*([0-9]+)$", "$1");
        
        LegalMessageSentDetails.EventCodeEnum eventCode = null;
        
        if (address != null) {
            String domainPart = address.replaceFirst(".*@", "");

            if (domainPart.startsWith(EXT_CHANNEL_SEND_FAIL_BOTH)
                    || (domainPart.startsWith(EXT_CHANNEL_SEND_FAIL_FIRST) && "0".equals(retryNumberPart))) {
                status = ProgressEventCategory.ERROR;
                eventCode = LegalMessageSentDetails.EventCodeEnum.C004;
            } else if (domainPart.startsWith(EXT_CHANNEL_WORKS) || domainPart.startsWith(EXT_CHANNEL_SEND_FAIL_FIRST)) {
                status = ProgressEventCategory.OK;
                eventCode = LegalMessageSentDetails.EventCodeEnum.C003;
            } else {
                throw new IllegalArgumentException("PecAddress " + address + " do not match test rule for mocks");
            }
        } else {
            throw new IllegalArgumentException("PecAddress is null");
        }

        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();

        LegalMessageSentDetails extChannelResponse = new LegalMessageSentDetails();
        extChannelResponse.setStatus(status);
        extChannelResponse.setEventTimestamp(ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toOffsetDateTime());
        extChannelResponse.setRequestId(timelineEventId);
        extChannelResponse.setEventCode(eventCode); //AVVENUTA CONSEGNA
        extChannelResponse.setGeneratedMessage(
                new DigitalMessageReference()
                        .id("test_id")
                        .location("safestorage://urlditest")
                        .system("test_system")
        );
        
        singleStatusUpdate.setDigitalLegal(extChannelResponse);

        externalChannelHandler.extChannelResponseReceiver(singleStatusUpdate);
    }
}
