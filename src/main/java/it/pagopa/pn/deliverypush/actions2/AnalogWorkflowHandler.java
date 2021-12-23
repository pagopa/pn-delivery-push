package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.api.dto.publicregistry.PhysicalAddressDTO;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public class AnalogWorkflowHandler {
    private TimelineService timelineService;
    private NotificationDao notificationDao;
    private PublicRegistryHandler publicRegistryHandler;
    private ExternalChannel externalChannel;
    private CompletionWorkFlow completionWorkFlow;

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
            NotificationRecipient recipient = null; //TODO Ottiene il recipient dalla notifica

            int sentAttempt = getSentAttemptFromTimeLine(iun, taxId);

            switch (sentAttempt) {
                case 0:
                    PhysicalAddress paProvidedAddress = recipient.getPhysicalAddress();

                    if (paProvidedAddress != null && paProvidedAddress.getAddress() != null) {
                        //send notification with paAddress
                        sendNotificationToExternalChannel(iun, notification, recipient, paProvidedAddress, true);
                    } else {
                        //Get address for notification from public registry
                        getAddressFromPublicRegistry(iun, taxId, sentAttempt);
                    }
                    break;
                case 1:
                    //An send attempt was already made, get address from public registry for second send attempt
                    getAddressFromPublicRegistry(iun, taxId, sentAttempt);
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
        publicRegistryHandler.sendRequestForGetAddress(iun, taxId, correlationId, DeliveryMode.ANALOG, ContactPhase.SEND_ATTEMPT);
    }

    private void sendNotificationToExternalChannel(String iun, Notification notification, NotificationRecipient recipient, PhysicalAddress address, boolean investigation) {
        externalChannel.sendAnalogNotification(notification, address, iun, recipient, investigation);
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
            NotificationRecipient recipient = null; //TODO Ottiene il recipient dalla notifica

            int numberOfSendAttempt = getSentAttemptFromTimeLine(iun, taxId);

            switch (numberOfSendAttempt) {
                case 0:
                    //Send notification to external channel if response address is available
                    sendNotificationIfAddressIsAvailable(iun, taxId, response, notification, recipient);
                    break;
                case 1:
                    //Ottenuta risposta alla seconda send
                    handleSecondSendResponse(iun, taxId, response, notification, recipient);
                    break;
                case 2:
                    //TODO Verificare se qui può mai arrivare
                    unreachableUser(iun, taxId);
                    break;
                default:
                    //TODO Gestire errore
                    break;
            }
        } else {
            //Gestire casistica di errore
        }

    }

    private void sendNotificationIfAddressIsAvailable(String iun, String taxId, PublicRegistryResponse response, Notification notification, NotificationRecipient recipient) {
        if (response.getPhysicalAddress() != null && response.getPhysicalAddress().getAddress() != null) {
            // Send notification to external channel If response address is available
            PhysicalAddress physicalAddress = mapResponseAddressToPhysicalAddress(response.getPhysicalAddress());
            sendNotificationToExternalChannel(iun, notification, recipient, physicalAddress, true);
        } else {
            //there isn't available address for the user so is not reachable
            unreachableUser(iun, taxId);
        }
    }

    private void handleSecondSendResponse(String iun, String taxId, PublicRegistryResponse response, Notification notification, NotificationRecipient recipient) {
        //Get external channel last feedback information returned from timeline
        SendPaperFeedbackDetails lastSentFeedback = getTimelineSentFeedback(iun, taxId);
        PhysicalAddress lastUsedAddress = lastSentFeedback.getAddress();

        if (response.getPhysicalAddress() != null && response.getPhysicalAddress().getAddress() != null) {

            PhysicalAddress responseAddress = mapResponseAddressToPhysicalAddress(response.getPhysicalAddress());
            //check if response address is different to last used address to avoid resending failure notification to the same address
            if (!responseAddress.equals(lastUsedAddress)) { //TODO Da definire in maniera chiara il metodo equals
                sendNotificationToExternalChannel(iun, notification, recipient, responseAddress, false);
            } else {
                //Send notification with investigation address if it is available
                checkInvestigationAddressAndSend(iun, taxId, notification, recipient, lastSentFeedback.getNewAddress());
            }
        } else {
            //Send notification with investigation address if it is available
            checkInvestigationAddressAndSend(iun, taxId, notification, recipient, lastSentFeedback.getNewAddress());
        }
    }


    /**
     * If during last failed sent notification a new address has been obtained send notification else the user is unreachable
     */
    private void checkInvestigationAddressAndSend(String iun, String taxId, Notification notification, NotificationRecipient recipient, PhysicalAddress newAddress) {
        if (newAddress != null && newAddress.getAddress() != null) {
            sendNotificationToExternalChannel(iun, notification, recipient, newAddress, false);
        } else {
            // the user is not reachable
            unreachableUser(iun, taxId);
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

    private PhysicalAddress mapResponseAddressToPhysicalAddress(PhysicalAddressDTO physicalAddress) {
        //TODO DA Implementare
        throw new UnsupportedOperationException();
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

}
