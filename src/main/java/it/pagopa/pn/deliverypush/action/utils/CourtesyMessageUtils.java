package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.io.IoSendMessageResultInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ProbableDateAnalogWorkflowDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendCourtesyMessageDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.model.SendMessageRequestBody;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.emdintegration.PnEmdIntegrationClient;
import it.pagopa.pn.deliverypush.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ERRORCOURTESY;
import static it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageResponse.ResultEnum.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class CourtesyMessageUtils {
    private final AddressBookService addressBookService;
    private final ExternalChannelService externalChannelService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationUtils notificationUtils;
    private final IoService iOservice;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final PnEmdIntegrationClient pnEmdIntegrationClient;
    private final AuditLogService auditLogService;

    /**
     * Get recipient addresses and send courtesy messages.
     * @return report of sent courtesy messages and scheduling analog date if applicable.
     */
    public CourtesyMessagesReport checkAddressesAndSendCourtesyMessage(NotificationInt notification, Integer recIndex, DeliveryModeInt deliveryMode) {
        final String iun = notification.getIun();
        log.debug("Start checkAddressesForSendCourtesyMessage - iun={} id={} delivery mode={} ", iun, recIndex, deliveryMode);

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);

        // Ottiene tutti gli indirizzi di cortesia per il recipient
        List<CourtesyDigitalAddressInt> listCourtesyAddresses = addressBookService.getCourtesyAddress(recipient.getInternalId(), notification.getSender().getPaId());

        CourtesyMessagesReport courtesyMessagesReport = new CourtesyMessagesReport();
        Instant probableSchedulingAnalogDate = retrieveOrCalculateSchedulingAnalogDate(iun , recIndex);

        for (CourtesyDigitalAddressInt courtesyAddress : listCourtesyAddresses) {
            try {
                if (trySendCourtesyMessage(notification, recIndex, courtesyAddress, probableSchedulingAnalogDate, deliveryMode)) {
                    courtesyMessagesReport.addSentCourtesyType(courtesyAddress.getType());
                } else {
                    courtesyMessagesReport.addNotSentCourtesyType(courtesyAddress.getType());
                }
            } catch (Exception ex) {
                //Se l'invio del messaggio di cortesia fallisce per un qualsiasi motivo il processo non si blocca. Viene fatto catch exception e loggata
                log.error("Exception in send courtesy message, courtesyType={} ex={} - iun={} id={}", courtesyAddress.getType(), ex, notification.getIun(), recIndex);
                courtesyMessagesReport.addCourtesyTypeInError(courtesyAddress.getType());
            }
        }

        if (courtesyMessagesReport.hasSentAtLeastACourtesyMessage()) {
            addProbableSchedulingElementToTimeline(notification, recIndex, probableSchedulingAnalogDate);
            courtesyMessagesReport.setSchedulingAnalogDate(probableSchedulingAnalogDate);
        }

        log.debug("End sendCourtesyMessage - IUN={} id={}", iun, recIndex);
        return courtesyMessagesReport;
    }

    private Instant retrieveOrCalculateSchedulingAnalogDate(String iun, Integer recIndex) {
        // Provo a recuperare la data dalla timeline
        String probableSchedulingElementId = getProbableSchedulingAnalogTimelineElementId(recIndex, iun);
        Instant schedulingAnalogDate = retrieveProbableSchedulingAnalogTimeline(iun, probableSchedulingElementId);
        if (schedulingAnalogDate != null) {
            log.info("Scheduling analog date found in timeline - iun={} id={} schedulingAnalogDate={}", iun, recIndex, schedulingAnalogDate);
            return schedulingAnalogDate;
        }
        // Se non esiste, la calcolo ex-novo
        Duration waitingTime = pnDeliveryPushConfigs.getTimeParams().getWaitingForReadCourtesyMessage();
        log.info("Scheduling analog date not found in timeline, calculating new one - iun={} id={} waitingTime={}", iun, recIndex, waitingTime);
        return Instant.now().plus(waitingTime);
    }

    /**
     * Tenta di inviare il messaggio di cortesia specifico in base al tipo.
     * @return true se il messaggio è stato inviato con successo.
     */
    private boolean trySendCourtesyMessage(NotificationInt notification,
                                           Integer recIndex,
                                           CourtesyDigitalAddressInt courtesyAddress,
                                           Instant schedulingAnalogDate,
                                           DeliveryModeInt deliveryMode) {

        log.debug("Send courtesy message attempt for address type {} - iun={} id={}", courtesyAddress.getType(), notification.getIun(), recIndex);

        if (timelineUtils.checkIsNotificationCancellationRequested(notification.getIun())) {
            log.warn("{} courtesy blocked for cancelled notification iun={}", courtesyAddress.getType(), notification.getIun());
            return false;
        }

        boolean messageSent = false;
        switch (courtesyAddress.getType()) {
            case EMAIL, SMS -> messageSent = manageCourtesyMessage(notification, recIndex, courtesyAddress, deliveryMode);
            case APPIO -> messageSent = manageIOMessage(notification, recIndex, courtesyAddress, schedulingAnalogDate, deliveryMode);
            case TPP -> messageSent = manageTPPMessage(notification, recIndex, courtesyAddress);
            default -> handleCourtesyTypeError(notification, recIndex, courtesyAddress);
        }

        return messageSent;
    }

    private void handleCourtesyTypeError(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt courtesyAddress) {
        log.error("Is not possibile to send courtesy message, courtesyAddressType={} is not defined - iun={} id={}",
                courtesyAddress.getType(), notification.getIun(), recIndex);
        throw new PnInternalException("Is not possibile to send courtesy message, courtesyAddressType=" + courtesyAddress.getType() +
                " is not defined - iun=" + notification.getIun() + " id=" + recIndex, ERROR_CODE_DELIVERYPUSH_ERRORCOURTESY);
    }

    // --- Timeline and Utility Methods ---

    private Instant retrieveProbableSchedulingAnalogTimeline(String iun, String elementId) {
        return timelineService.getTimelineElementDetails(iun, elementId, ProbableDateAnalogWorkflowDetailsInt.class)
                .map(ProbableDateAnalogWorkflowDetailsInt::getSchedulingAnalogDate)
                .orElseGet(() -> {
                    log.info("[{}] ProbableSchedulingDateAnalogWorkflowElement is not present for elementId: {}", iun, elementId);
                    return null;
                });
    }

    public static String getSendCourtesyTimelineElementId(Integer recIndex, String iun, CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT courtesyAddressType, Boolean optin) {
        return TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(recIndex)
                .courtesyAddressType(courtesyAddressType)
                .optin(optin)
                .build()
        );
    }

    private String getProbableSchedulingAnalogTimelineElementId(Integer recIndex, String iun) {
        return TimelineEventId.PROBABLE_SCHEDULING_ANALOG_DATE.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(recIndex)
                .build()
        );
    }

    public void addSendCourtesyMessageToTimeline(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt courtesyAddress, Instant sentDate) {
        this.addSendCourtesyMessageToTimeline(notification, recIndex, courtesyAddress, sentDate, getSendCourtesyTimelineElementId(recIndex, notification.getIun(), courtesyAddress.getType(), Boolean.FALSE),
                IoSendMessageResultInt.SENT_COURTESY);
    }

    private void addSendCourtesyMessageToTimeline(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt courtesyAddress, Instant sentDate, String eventId,
                                                  IoSendMessageResultInt ioSendMessageResult) {
        addTimelineElement(
                timelineUtils.buildSendCourtesyMessageTimelineElement(recIndex, notification, courtesyAddress, sentDate, eventId, ioSendMessageResult),
                notification
        );
    }

    private void addProbableSchedulingElementToTimeline(NotificationInt notification, int recIndex, Instant schedulingAnalogDate) {
        String timelineElementId = getProbableSchedulingAnalogTimelineElementId(recIndex, notification.getIun());
        addTimelineElement(
                timelineUtils.buildProbableDateSchedulingAnalogTimelineElement(recIndex, notification, timelineElementId, schedulingAnalogDate),
                notification
        );
    }

    public List<SendCourtesyMessageDetailsInt> getSentCourtesyMessagesDetails(String iun, int recIndex) {
        // cerco dal DB tutte le timeline relative a iun/recindex che sono di tipo courtesy
        String elementIdForSearch = TimelineEventId.SEND_COURTESY_MESSAGE
                .buildSearchEventIdByIunAndRecipientIndex(iun, recIndex);

        return this.timelineService.getTimelineByIunTimelineId(iun, elementIdForSearch, false)
                .stream().map(x -> (SendCourtesyMessageDetailsInt) x.getDetails()).toList();
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

    // --- Gestori dei Messaggi ---

    private boolean manageCourtesyMessage(NotificationInt notification, int recIndex, CourtesyDigitalAddressInt courtesyAddress, DeliveryModeInt deliveryMode) {
        log.info("Send courtesy message to externalChannel courtesyType={} - iun={} id={} ", courtesyAddress.getType(), notification.getIun(), recIndex);

        String eventId = getSendCourtesyTimelineElementId(recIndex, notification.getIun(), courtesyAddress.getType(), Boolean.FALSE);
        externalChannelService.sendCourtesyNotification(notification, courtesyAddress, recIndex, eventId, deliveryMode);
        addSendCourtesyMessageToTimeline(notification, recIndex, courtesyAddress, Instant.now(), eventId, null);
        return true;
    }

    private boolean manageIOMessage(NotificationInt notification, int recIndex, CourtesyDigitalAddressInt courtesyAddress, Instant schedulingAnalogDate, DeliveryModeInt deliveryMode) {
        // nel caso di IO, il messaggio potrebbe NON essere inviato. Al netto del fatto di eccezioni, che vengono catchate sotto
        // ci sono casi in cui non viene inviato perchè l'utente non ha abilitato IO. Quindi in questi casi non viene salvato l'evento di timeline
        // NB: anche nel caso di invio di Opt-in, non salvo l'evento in timeline.
        log.info("Send courtesy message to App IO - iun={} id={} ", notification.getIun(), recIndex);

        SendMessageResponse.ResultEnum result = iOservice.sendIOMessage(notification, recIndex, schedulingAnalogDate, deliveryMode);

        if (SENT_COURTESY.equals(result) || SENT_OPTIN.equals(result) || NOT_SENT_OPTIN_ALREADY_SENT.equals(result)) {
            // Se l'invio ha avuto successo o ha gestito l'opt-in, salviamo l'evento in timeline
            IoSendMessageResultInt ioSendMessageResult = IoSendMessageResultInt.valueOf(result.getValue());
            boolean isOptin = SENT_OPTIN.equals(result) || NOT_SENT_OPTIN_ALREADY_SENT.equals(result);
            String eventId = getSendCourtesyTimelineElementId(recIndex, notification.getIun(), courtesyAddress.getType(), isOptin);

            addSendCourtesyMessageToTimeline(notification, recIndex, courtesyAddress, Instant.now(), eventId, ioSendMessageResult);
            return true;
        } else {
            log.info("skipping saving courtesy timeline iun={} id={}", notification.getIun(), recIndex);
            return false;
        }
    }

    private boolean manageTPPMessage(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt courtesyAddress) {
        final String iun = notification.getIun();
        log.info("manageTPPMessage - iun={} id={} ", iun, recIndex);

        String eventId = getSendCourtesyTimelineElementId(recIndex, iun, courtesyAddress.getType(), Boolean.FALSE);
        SendMessageRequestBody request = buildSendMessageRequest(notification, recIndex);
        PnAuditLogEvent logEvent = buildAuditLogEvent(iun, recIndex, eventId);

        it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.model.SendMessageResponse response = pnEmdIntegrationClient.sendMessage(request);

        if (response.getOutcome() == it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.model.SendMessageResponse.OutcomeEnum.OK) {
            addSendCourtesyMessageToTimeline(notification, recIndex, courtesyAddress, Instant.now(), eventId, null);
            logEvent.generateSuccess("successful sent courtesy message via TPP channel with recIndex ={} and iun ={}", recIndex, iun).log();
            return true;
        } else {
            logEvent.generateSuccess("TPP channel not enabled for recipient with recIndex={} and iun={}", recIndex, iun).log();
            return false;
        }
    }

    private SendMessageRequestBody buildSendMessageRequest(NotificationInt notification, Integer recIndex) {
        return new SendMessageRequestBody()
                .recipientId(notification.getRecipients().get(recIndex).getTaxId())
                .internalRecipientId(notification.getRecipients().get(recIndex).getInternalId())
                .originId(notification.getIun())
                .senderDescription(notification.getSender().getPaDenomination())
                .associatedPayment(hasRecipientPagoPaPayment(notification, recIndex));
    }

    private PnAuditLogEvent buildAuditLogEvent(String iun, int recIndex, String eventId) {
        return auditLogService.buildAuditLogEvent(iun, recIndex, PnAuditLogEventType.AUD_DA_SEND_TPP, "sendTppMessage eventId={}", eventId);
    }

    private boolean hasRecipientPagoPaPayment(NotificationInt notification, Integer recIndex) {
        if(CollectionUtils.isEmpty(notification.getRecipients().get(recIndex).getPayments())) {
            return false;
        }
        return notification.getRecipients().get(recIndex).getPayments().stream()
                .anyMatch(paymentInfo -> paymentInfo.getPagoPA() != null);
    }

}
