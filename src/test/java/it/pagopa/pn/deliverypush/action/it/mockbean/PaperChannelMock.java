package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelPrepareRequest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PaperChannelResponseHandler;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.await;

@Slf4j
public class PaperChannelMock implements PaperChannelSendClient {

    //ANALOG
    public static final String EXTCHANNEL_SEND_SUCCESS = "OK"; //Invio notifica ok
    public static final String EXTCHANNEL_SEND_FAIL = "FAIL"; //Invio notifica fallita
    public static final String EXTCHANNEL_SEND_FAIL_KOUNREACHABLE = "KOUNREACHABLE"; //Invio notifica fallita
    public static final String EXT_CHANNEL_SEND_NEW_ADDR = "NEW_ADDR:"; //Invio notifica fallita con nuovo indirizzo da investigazione
    //Esempio: La combinazione di EXT_CHANNEL_SEND_NEW_ADDR + EXTCHANNEL_SEND_OK ad esempio significa -> Invio notifica fallito ma con nuovo indirizzo trovato e l'invio a tale indirzzo avrà successo

    public static final int WAITING_TIME = 100;
    private static final Pattern NEW_ADDRESS_INPUT_PATTERN = Pattern.compile("^" + EXT_CHANNEL_SEND_NEW_ADDR + "(.*)$");
    public static final String PAPER_ADDRESS_FULL_NAME = "full name";
    public static final String PAPER_ADDRESS_CITTA = "citta";
    public static final String PAPER_ADDRESS_ITALY = "italy";

    private final PaperChannelResponseHandler paperChannelResponseHandler;
    private final TimelineService timelineService;

    public PaperChannelMock(@Lazy PaperChannelResponseHandler paperChannelResponseHandler,
                            @Lazy TimelineService timelineService) {
        this.paperChannelResponseHandler = paperChannelResponseHandler;
        this.timelineService = timelineService;
    }

    @Override
    public void prepare(PaperChannelPrepareRequest paperChannelPrepareRequest) {
        log.info("[TEST] prepare paperChannelPrepareRequest:{}", paperChannelPrepareRequest);

        new Thread(() -> {
            await().pollDelay(Duration.ofMillis(200)).atMost(Duration.ofSeconds(30)).untilAsserted(() ->

                    Assertions.assertTrue(true)

            );

            Assertions.assertDoesNotThrow(() -> {
                String address = paperChannelPrepareRequest.getDiscoveredAddress()!=null?paperChannelPrepareRequest.getDiscoveredAddress().getAddress():null;
                if (address == null)
                    address = paperChannelPrepareRequest.getPaAddress()!=null?paperChannelPrepareRequest.getPaAddress().getAddress():null;
                simulatePrepareResponse(paperChannelPrepareRequest.getRequestId(), address);
            });

        }).start();
    }

    @Override
    public SendResponse send(PaperChannelSendRequest paperChannelSendRequest) {
        //TODO Da eliminare questo log
        log.info("[TEST] send paperChannelSendRequest:{}", paperChannelSendRequest);


        new Thread(() -> {
            await().pollDelay(Duration.ofMillis(200)).atMost(Duration.ofSeconds(30)).untilAsserted(() ->

                    Assertions.assertTrue(true)

            );

            Assertions.assertDoesNotThrow(() -> {
                simulateSendResponse(paperChannelSendRequest.getRequestId(), paperChannelSendRequest.getReceiverAddress().getAddress());
            });

        }).start();

        return new SendResponse()
                .amount(100)
                .numberOfPages(1)
                .envelopeWeight(100);
    }


    private void simulatePrepareResponse(String timelineEventId,  String address) {
        PaperChannelUpdate singleStatusUpdate = new PaperChannelUpdate();
        PrepareEvent prepareEvent = new PrepareEvent();
        prepareEvent.setStatusDateTime(Instant.now());
        prepareEvent.setRequestId(timelineEventId);

        String status = "PROGRESS";
        if (address == null)
        {
            status = "KOUNREACHABLE";
        }
        else
        {
            Matcher matcher = NEW_ADDRESS_INPUT_PATTERN.matcher(address);
            if (matcher.find()) {
                status = "OK";
            } else if (address.startsWith(EXTCHANNEL_SEND_FAIL_KOUNREACHABLE)) {
                status = "KOUNREACHABLE";
            } else if (address.startsWith(EXTCHANNEL_SEND_SUCCESS)) {
                status = "OK";
            }  else if (address.startsWith(EXTCHANNEL_SEND_FAIL)) {
                status = "OK";
            } else {
                throw new IllegalArgumentException("Address " + address + " do not match test rule for mocks");
            }
        }



        if (status.equals("OK")) {
            prepareEvent.setReceiverAddress(new AnalogAddress());
            prepareEvent.getReceiverAddress().setFullname(PAPER_ADDRESS_FULL_NAME);
            prepareEvent.getReceiverAddress().setAddress(address);
            prepareEvent.getReceiverAddress().setCity(PAPER_ADDRESS_CITTA);
            prepareEvent.getReceiverAddress().setCountry(PAPER_ADDRESS_ITALY);

            prepareEvent.setProductType("NR_AR");
        }

        singleStatusUpdate.setPrepareEvent(prepareEvent);
        prepareEvent.setStatusCode(StatusCodeEnum.valueOf(status));
        
        paperChannelResponseHandler.paperChannelResponseReceiver(singleStatusUpdate);
    }

    private void simulateSendResponse(String timelineEventId, String address) {
        PaperChannelUpdate singleStatusUpdate = new PaperChannelUpdate();
        SendEvent sendEvent = new SendEvent();
        sendEvent.setStatusDateTime(Instant.now());
        sendEvent.setRequestId(timelineEventId);

        String newAddress;
        StatusCodeEnum status = null;
        Matcher matcher = NEW_ADDRESS_INPUT_PATTERN.matcher(address);
        if (matcher.find()) {
            status = StatusCodeEnum.KO;
            newAddress = matcher.group(1).trim();
        } else if (address.startsWith(EXTCHANNEL_SEND_FAIL)) {
            status = StatusCodeEnum.KO;
            sendEvent.setStatusDetail("errore fail mock!");
            newAddress = null;
        } else if (address.startsWith(EXTCHANNEL_SEND_SUCCESS)) {
            status = StatusCodeEnum.OK;
            newAddress = null;
        } else {
            throw new IllegalArgumentException("Address " + address + " do not match test rule for mocks");
        }


        if (newAddress != null) {
            sendEvent.setDiscoveredAddress(new AnalogAddress());
            sendEvent.getDiscoveredAddress().setFullname(PAPER_ADDRESS_FULL_NAME);
            sendEvent.getDiscoveredAddress().setAddress(newAddress);
            sendEvent.getDiscoveredAddress().setCity(PAPER_ADDRESS_CITTA);
            sendEvent.getDiscoveredAddress().setCountry(PAPER_ADDRESS_ITALY);
        }

        sendEvent.setStatusCode(status);


        singleStatusUpdate.setSendEvent(sendEvent);

        paperChannelResponseHandler.paperChannelResponseReceiver(singleStatusUpdate);
    }

}
