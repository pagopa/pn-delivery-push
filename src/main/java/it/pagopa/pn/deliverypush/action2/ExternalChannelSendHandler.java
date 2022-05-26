package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.AarGenerationDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class ExternalChannelSendHandler {
    private final ExternalChannelUtils externalChannelUtils;
    private final ExternalChannelSendClient externalChannel;
    private final NotificationUtils notificationUtils;
    private final TimelineService timelineService;

    public ExternalChannelSendHandler(ExternalChannelUtils externalChannelUtils, ExternalChannelSendClient externalChannel, NotificationUtils notificationUtils, TimelineService timelineService) {
        this.externalChannelUtils = externalChannelUtils;
        this.externalChannel = externalChannel;
        this.notificationUtils = notificationUtils;
        this.timelineService = timelineService;
    }

    /**
     * Send pec notification to external channel
     * MEssaggio con valore legale (PEC)
     */
    public void sendDigitalNotification(NotificationInt notification, DigitalAddress digitalAddress, DigitalAddressSource addressSource, Integer recIndex,
                                        int sentAttemptMade) {

        String eventId = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .index(sentAttemptMade)
                        .build()
        );

        externalChannelUtils.addSendDigitalNotificationToTimeline(notification, digitalAddress, addressSource, recIndex, sentAttemptMade, eventId);

        externalChannel.sendDigitalNotification(notification, digitalAddress, eventId);
    }

    /**
     * Send courtesy message to external channel
     * MEssaggio senza valore legale (EMAIL, SMS)
     *
     */
    public void sendCourtesyNotification(NotificationInt notification, DigitalAddress courtesyAddress, Integer recIndex, String eventId) {

        externalChannelUtils.addSendCourtesyMessageToTimeline(notification, courtesyAddress, recIndex, eventId);

        externalChannel.sendDigitalNotification(notification, courtesyAddress, eventId);
    }

    /**
     * Send registered letter to external channel
     * to use when all pec send fails
     * Invio di RACCOMANDATA SEMPLICE quando falliscono tutti i tentativi via PEC
     */
    public void sendNotificationForRegisteredLetter(NotificationInt notification, PhysicalAddress physicalAddress, Integer recIndex) {
         String eventId = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );

        externalChannelUtils.addSendSimpleRegisteredLetterToTimeline(notification, physicalAddress, recIndex, eventId);

        // la tipologia qui è sempre raccomandata semplice SR
        externalChannel.sendAnalogNotification(notification, notificationUtils.getRecipientFromIndex(notification,recIndex), eventId,
                ExternalChannelSendClient.ANALOG_TYPE.SIMPLE_REGISTERED_LETTER, getAarPdfFromTimeline(notification, recIndex));
    }

    /**
     * Send paper notification to external channel
     * AR o 890
     *
     */
    public void sendAnalogNotification(NotificationInt notification, PhysicalAddress physicalAddress, Integer recIndex, boolean investigation, int sentAttemptMade) {
        String eventId = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .index(sentAttemptMade)
                        .build()
        );

        externalChannelUtils.addSendAnalogNotificationToTimeline(notification, physicalAddress, recIndex, investigation, sentAttemptMade, eventId);

        // c'è una discrepanza nella nomenclatura tra l'enum serviceleveltype.SIMPLE_REGISTERED_LETTER che si traduce in AR e non SR.
        // Cmq se non è 890, si intende AR.
        externalChannel.sendAnalogNotification(notification, notificationUtils.getRecipientFromIndex(notification,recIndex), eventId,
                notification.getPhysicalCommunicationType()== ServiceLevelTypeInt.REGISTERED_LETTER_890?ExternalChannelSendClient.ANALOG_TYPE.REGISTERED_LETTER_890:ExternalChannelSendClient.ANALOG_TYPE.AR_REGISTERED_LETTER
                , getAarPdfFromTimeline(notification, recIndex));
    }

    private String getAarPdfFromTimeline(NotificationInt notification, Integer recIndex) {
        // ricostruisco il timelineid della  genrazione dell'aar
        String aarGenerationEventId = TimelineEventId.AAR_GENERATION.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );

        Optional<AarGenerationDetails> detail = timelineService
                    .getTimelineElementDetails(notification.getIun(), aarGenerationEventId, AarGenerationDetails.class);

        if (detail.isEmpty())
            throw new PnInternalException("cannot retreieve AAR pdf safestoragekey");

        return detail.get().getSafestorageKey();
    }
}
