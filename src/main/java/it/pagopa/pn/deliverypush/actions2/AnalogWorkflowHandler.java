package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.api.dto.publicregistry.PhysicalAddressDTO;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;

import java.util.Optional;
import java.util.Set;

public class AnalogWorkflowHandler {
    private TimelineService timelineService;
    private NotificationDao notificationDao;
    private PublicRegistryHandler publicRegistryHandler;
    private ExternalChannel externalChannel;

    public void analogWorkflowHandler(String iun, String taxId) {
        Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);
        if (optNotification.isPresent()) {
            Notification notification = optNotification.get();
            NotificationRecipient recipient = null; //TODO Ottiene il recipient dalla notifica

            int numberOfSendAttempt = getNumberOfSendAttemptFromTimeLine(iun, taxId);
            switch (numberOfSendAttempt) {
                case 0:
                    PhysicalAddress paProvidedAddress = recipient.getPhysicalAddress();

                    if (paProvidedAddress != null && paProvidedAddress.getAddress() != null) {
                        sendNotificationToExternalChannel(iun, notification, recipient, paProvidedAddress, true);
                    } else {
                        publicRegistryCall(iun, taxId, numberOfSendAttempt);
                    }
                    break;
                case 1:
                    publicRegistryCall(iun, taxId, numberOfSendAttempt);
                    break;
                case 2:
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

    private void publicRegistryCall(String iun, String taxId, int numberOfSendAttempt) {
        String correlationId = iun + "_" + taxId + "_" + "analog" + "_" + numberOfSendAttempt;
        publicRegistryHandler.sendNotification(iun, taxId, correlationId, DeliveryMode.ANALOG, ContactPhase.SEND_ATTEMPT);
    }

    private void sendNotificationToExternalChannel(String iun, Notification notification, NotificationRecipient recipient, PhysicalAddress address, boolean investigation) {
        externalChannel.sendAnalogNotification(notification, address, iun, recipient, investigation);
    }

    private int getNumberOfSendAttemptFromTimeLine(String iun, String taxId) {
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

    private void unreachableUser(String iun, String taxId) {
    }

    public void handlePublicRegistryResponse(String iun, String taxId, PublicRegistryResponse response) {
        Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);
        if (optNotification.isPresent()) {
            Notification notification = optNotification.get();
            NotificationRecipient recipient = null; //TODO Ottiene il recipient dalla notifica

            int numberOfSendAttempt = getNumberOfSendAttemptFromTimeLine(iun, taxId);
            switch (numberOfSendAttempt) {
                case 0:
                    //Ottenuta risposta alla prima send
                    handleFirstSendResponse(iun, taxId, response, notification, recipient);
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

    private void handleSecondSendResponse(String iun, String taxId, PublicRegistryResponse response, Notification notification, NotificationRecipient recipient) {
        SendPaperFeedbackDetails lastSentFeedback = getTimelineSentFeedback(iun, taxId);
        PhysicalAddress lastUsedAddress = lastSentFeedback.getAddress();

        if (response.getPhysicalAddress() != null && response.getPhysicalAddress().getAddress() != null) {
            PhysicalAddress responseAddress = mapResponseAddressToPhysicalAddress(response.getPhysicalAddress());
            if (!responseAddress.equals(lastUsedAddress)) { //TODO Da definire in maniera chiara il metodo equals
                sendNotificationToExternalChannel(iun, notification, recipient, responseAddress, false);
            } else {
                checkInvestigationAddressAndSend(iun, taxId, notification, recipient, lastSentFeedback);
            }
        } else {
            checkInvestigationAddressAndSend(iun, taxId, notification, recipient, lastSentFeedback);
        }
    }

    private void handleFirstSendResponse(String iun, String taxId, PublicRegistryResponse response, Notification notification, NotificationRecipient recipient) {
        if (response.getPhysicalAddress() != null && response.getPhysicalAddress().getAddress() != null) {
            PhysicalAddress physicalAddress = mapResponseAddressToPhysicalAddress(response.getPhysicalAddress());
            sendNotificationToExternalChannel(iun, notification, recipient, physicalAddress, true);
        } else {
            unreachableUser(iun, taxId);
        }
    }

    private void checkInvestigationAddressAndSend(String iun, String taxId, Notification notification, NotificationRecipient recipient, SendPaperFeedbackDetails lastSentFeedback) {
        if (lastSentFeedback.getNewAddress() != null && lastSentFeedback.getNewAddress().getAddress() != null) {
            sendNotificationToExternalChannel(iun, notification, recipient, lastSentFeedback.getNewAddress(), false);
        } else {
            unreachableUser(iun, taxId);
        }
    }

    private SendPaperFeedbackDetails getTimelineSentFeedback(String iun, String taxId) {
        
    }

    private PhysicalAddress mapResponseAddressToPhysicalAddress(PhysicalAddressDTO physicalAddress) {
        //TODO DA Implementare
        throw new UnsupportedOperationException();
    }

}
