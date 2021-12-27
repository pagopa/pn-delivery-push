package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

public class AnalogWorkflowHandler {
    private TimelineService timelineService;
    private NotificationDao notificationDao;
    private PublicRegistryService publicRegistryService;
    private ExternalChannelService externalChannelService;
    private CompletionWorkFlowHandler completionWorkFlow;
    private CourtesyMessageService courtesyMessageService;
    private SchedulerService scheduler;

    /**
     * Start analog workflow, if courtesy message has been sent to the user, it is necessary to wait 5 days (from sent message date) before start Analog workflow
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    public void startAnalogWorkflow(String iun, String taxId) {
        Optional<SendCourtesyMessageDetails> SendCourtesyMessageDetailsOpt = courtesyMessageService.getFirstSentCourtesyMessage(iun, taxId);

        if (SendCourtesyMessageDetailsOpt.isPresent()) {
            SendCourtesyMessageDetails sendCourtesyMessageDetails = SendCourtesyMessageDetailsOpt.get();
            Instant schedulingDate = sendCourtesyMessageDetails.getSendDate().plus(5, ChronoUnit.DAYS);
            scheduler.schedulEvent(iun, taxId, schedulingDate, ActionType.ANALOG_WORKFLOW);
        } else {
            nextWorkflowAction(iun, taxId);
        }
    }

    /**
     * Handle analog notification Workflow based on already made attempt
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    public void nextWorkflowAction(String iun, String taxId) {
        Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);

        if (optNotification.isPresent()) {
            Notification notification = optNotification.get();
            NotificationRecipient recipient = getRecipientFromNotification(notification);

            int sentAttemptMade = getSentAttemptFromTimeLine(iun, taxId);

            switch (sentAttemptMade) {
                case 0:
                    PhysicalAddress paProvidedAddress = recipient.getPhysicalAddress();

                    if (paProvidedAddress != null) {
                        //send notification with paAddress
                        externalChannelService.sendAnalogNotification(notification, paProvidedAddress, recipient, true);
                    } else {
                        //Get address for notification from public registry
                        getAddressFromPublicRegistry(iun, taxId, sentAttemptMade);
                    }
                    break;
                case 1:
                    //An send attempt was already made, get address from public registry for second send attempt
                    getAddressFromPublicRegistry(iun, taxId, sentAttemptMade);
                    break;
                case 2:
                    // All sent attempts have already been made. The user is not reachable
                    unreachableUser(iun, taxId);
                    break;
                default:
                    //TODO Gestire errore
                    break;
            }

        } else {
            //TODO Gestire casistica di errore
        }

    }

    private void getAddressFromPublicRegistry(String iun, String taxId, int numberOfSendAttempt) {
        String correlationId = iun + "_" + taxId + "_" + "analog" + "_" + numberOfSendAttempt;
        publicRegistryService.sendRequestForGetAddress(iun, taxId, correlationId, DeliveryMode.ANALOG, ContactPhase.SEND_ATTEMPT);
    }

    /**
     * Get user sent attempt from timeline
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     * @return user sent attempt
     */
    private int getSentAttemptFromTimeLine(String iun, String taxId) {
        Set<TimelineElement> timeline = timelineService.getTimeline(iun);
        return (int) timeline.stream()
                .filter(timelineElement -> filterTimelineForTaxIdAndSource(timelineElement, taxId)).count();
    }

    private boolean filterTimelineForTaxIdAndSource(TimelineElement el, String taxId) {
        boolean availableAddressCategory = TimelineElementCategory.SEND_ANALOG_DOMICILE.equals(el.getCategory());
        if (availableAddressCategory) {
            SendPaperDetails details = (SendPaperDetails) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId());
        }
        return false;
    }

    /**
     * Handle get response for public registry call.
     *
     * @param iun      Notification unique identifier
     * @param taxId    User identifier
     * @param response public registry response
     */
    public void handlePublicRegistryResponse(String iun, String taxId, PublicRegistryResponse response) {
        Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);

        if (optNotification.isPresent()) {
            Notification notification = optNotification.get();
            NotificationRecipient recipient = getRecipientFromNotification(notification);

            int numberOfSendAttempt = getSentAttemptFromTimeLine(iun, taxId);

            switch (numberOfSendAttempt) {
                case 0:
                    //Send notification to external channel if response address is available
                    if (response.getPhysicalAddress() != null && response.getPhysicalAddress().getAddress() != null) {
                        // Send notification to external channel If response address is available
                        externalChannelService.sendAnalogNotification(notification, response.getPhysicalAddress(), recipient, true);
                    } else {
                        //there isn't available address for the user so is not reachable
                        unreachableUser(notification.getIun(), recipient.getTaxId());
                    }
                    break;
                case 1:
                    //Ottenuta risposta alla seconda send
                    handleSecondSendResponse(response, notification, recipient);
                    break;
                default:
                    //TODO Gestire errore
                    break;
            }
        } else {
            //Gestire casistica di errore
        }
    }

    private void handleSecondSendResponse(PublicRegistryResponse response, Notification notification, NotificationRecipient recipient) {
        String iun = notification.getIun();
        String taxId = recipient.getTaxId();

        //Get external channel last feedback information returned from timeline
        SendPaperFeedbackDetails lastSentFeedback = getTimelineSentFeedback(iun, taxId);
        PhysicalAddress lastUsedAddress = lastSentFeedback.getAddress();

        if (response.getPhysicalAddress() != null && response.getPhysicalAddress().getAddress() != null) {

            //check if response address is different to last used address to avoid resending failure notification to the same address
            if (!response.getPhysicalAddress().equals(lastUsedAddress)) { //TODO Da definire in maniera chiara il metodo equals
                externalChannelService.sendAnalogNotification(notification, response.getPhysicalAddress(), recipient, false);
            } else {
                //Send notification with investigation address if it is available
                checkInvestigationAddressAndSend(notification, recipient, lastSentFeedback.getNewAddress());
            }
        } else {
            //Send notification with investigation address if it is available
            checkInvestigationAddressAndSend(notification, recipient, lastSentFeedback.getNewAddress());
        }
    }


    /**
     * If during last failed sent notification a new address has been obtained send notification else the user is unreachable
     */
    private void checkInvestigationAddressAndSend(Notification notification, NotificationRecipient recipient, PhysicalAddress newAddress) {
        if (newAddress != null && newAddress.getAddress() != null) {
            externalChannelService.sendAnalogNotification(notification, newAddress, recipient, false);
        } else {
            // the user is not reachable
            unreachableUser(notification.getIun(), recipient.getTaxId());
        }
    }

    /**
     * Get external channel last feedback information from timeline
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     * @return last sent feedback information
     */
    private SendPaperFeedbackDetails getTimelineSentFeedback(String iun, String taxId) {
        Set<TimelineElement> timeline = timelineService.getTimeline(iun);

        Optional<SendPaperFeedbackDetails> sendPaperFeedbackDetailsOpt = timeline.stream()
                .filter(timelineElement -> filterLastAttemptDateInTimeline(timelineElement, taxId))
                .map(timelineElement -> (SendPaperFeedbackDetails) timelineElement.getDetails()).findFirst();

        if (sendPaperFeedbackDetailsOpt.isPresent()) {
            return sendPaperFeedbackDetailsOpt.get();
        } else {
            //TODO Gestisci casistica di errore
            throw new RuntimeException();
        }
    }

    private boolean filterLastAttemptDateInTimeline(TimelineElement el, String taxId) {
        boolean availableAddressCategory = TimelineElementCategory.SEND_PAPER_FEEDBACK.equals(el.getCategory());
        if (availableAddressCategory) {
            SendPaperFeedbackDetails details = (SendPaperFeedbackDetails) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId());
        }
        return false;
    }

    private void unreachableUser(String iun, String taxId) {
        //TODO Aggiungere alla tabella unreachableUser
        addUnreachableUserToTimeline(taxId);
        completionWorkFlow.completionAnalogWorkflow(taxId, iun, Instant.now(), EndWorkflowStatus.FAILURE);
    }

    private void addUnreachableUserToTimeline(String taxId) {
        timelineService.addTimelineElement(
                TimelineElement.builder()
                        .category(TimelineElementCategory.COMPLETELY_UNREACHABLE)
                        .details(CompletlyUnreachableDetails.builder()
                                .taxId(taxId)
                                .build()
                        )
                        .build()
        );
    }

    private NotificationRecipient getRecipientFromNotification(Notification notification) {
        //TODO Ottiene il recipient dalla notifica
        return null;
    }
}
