package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.addressbook.DigitalAddresses;
import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.AttemptAddressInfo;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource2;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook2;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
public class DigitalWorkFlowHandler {
    public static final int MAX_ATTEMPT_NUMBER = 2;

    private TimelineService timelineService;
    private CompletionWorkFlowHandler completionWorkFlow;
    private PublicRegistryService publicRegistryService;
    private AddressBook2 addressBook;
    private ExternalChannelService externalChannelService;
    private NotificationService notificationService;
    private SchedulerService scheduler;
    private DigitalService digitalService;

    /**
     * Handle digital notification Workflow based on already made attempt
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    public void nextWorkFlowAction(String iun, String taxId) {
        log.info("Next Digital workflow action for iun {} id {}", iun, taxId);

        //Viene ottenuta la source del prossimo indirizzo da testare, con il numero di tentativi già effettuati per tale sorgente e la data dell'ultimo tentativo
        AttemptAddressInfo nextAddressInfo = digitalService.getNextAddressInfo(iun, taxId);
        log.debug("Next address source is {} and attempt number already made is {}", nextAddressInfo.getAddressSource(), nextAddressInfo.getAttemptNumberMade());

        if (nextAddressInfo.getAttemptNumberMade() < MAX_ATTEMPT_NUMBER) {
            switch (nextAddressInfo.getAttemptNumberMade()) {
                case 0:
                    log.info("Start first attempt for source {}", nextAddressInfo.getAddressSource());
                    checkAndSendNotification(iun, taxId, nextAddressInfo);
                    break;
                case 1:
                    log.info("Start second attempt for source {}", nextAddressInfo.getAddressSource());
                    startNextWorkflow7daysAfterLastAttempt(iun, taxId, nextAddressInfo);
                    break;
                default:
                    log.error("Is not possibile to have {} number of attempt. Iun {} id {}", nextAddressInfo.getAttemptNumberMade(), iun, taxId);
                    throw new PnInternalException("Is not possibile to have " + nextAddressInfo.getAttemptNumberMade() + ". Iun " + iun + " id " + taxId);
            }
        } else {
            //Sono stati già effettuati tutti i tentativi possibili, la notificazione è quindi fallita
            log.info("Digital workflow is failed because all planned attempt have failed for iun {} id {}", iun, taxId);
            completionWorkFlow.completionDigitalWorkflow(taxId, iun, Instant.now(), EndWorkflowStatus.FAILURE);
        }
    }

    private void checkAndSendNotification(String iun, String taxId, AttemptAddressInfo nextAddressInfo) {

        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);
        log.debug("Get notification and recipient completed ");

        if (DigitalAddressSource2.GENERAL.equals(nextAddressInfo.getAddressSource())) {
            sendRequestForGetGeneralAddress(iun, taxId); //general address need async call to get it
        } else {
            sendNotificationOrStartNextWorkflowAction(nextAddressInfo.getAddressSource(), recipient, notification);
        }
    }

    /**
     * If for this address source 7 days has already passed since the last made attempt, for example because have already performed scheduling for previously
     * tried address, the notification step is called, else it is scheduled.
     *
     * @param iun             Notification unique identifier
     * @param taxId           User identifier
     * @param nextAddressInfo Next Address source information
     */
    private void startNextWorkflow7daysAfterLastAttempt(String iun, String taxId, AttemptAddressInfo nextAddressInfo) {
        Instant schedulingDate = nextAddressInfo.getLastAttemptDate().plus(7, ChronoUnit.DAYS);
        //Vengono aggiunti 7 giorni alla data dell'ultimo tentativo effettuata per questa source

        if (Instant.now().isAfter(schedulingDate)) {
            log.debug("Next workflow scheduling date {} is passed. Start next workflow ", schedulingDate);
            //Se la data odierna è successiva alla data ottenuta in precedenza, non c'è necessità di schedulare, perchè i 7 giorni necessari di attesa dopo il primo tentativo risultano essere già passati
            checkAndSendNotification(iun, taxId, nextAddressInfo);
        } else {
            log.debug("Next workflow scheduling date {} is not passed. Need to schedule next workflow ", schedulingDate);
            //Se la data è minore alla data odierna, bisogna attendere il completamento dei 7 giorni prima partire con un nuovo workflow per questa source
            scheduler.schedulEvent(iun, taxId, schedulingDate, ActionType.DIGITAL_WORKFLOW);
        }
    }

    private void sendNotificationOrStartNextWorkflowAction(DigitalAddressSource2 addressSource, NotificationRecipient recipient, Notification notification) {
        log.debug("Start sendNotificationOrStartNextWorkflowAction for addressSource {}", addressSource);

        //Viene ottenuto l'indirizzo a partire dalla source
        DigitalAddress destinationAddress = getAddressFromSource(addressSource, recipient, notification);

        //Viene Effettuato il check dell'indirizzo e l'eventuale send
        handleCheckAddressAndSend(recipient, notification, destinationAddress, addressSource);
    }

    @Nullable
    private DigitalAddress getAddressFromSource(DigitalAddressSource2 addressSource, NotificationRecipient recipient, Notification notification) {
        DigitalAddress destinationAddress;
        switch (addressSource) {
            case PLATFORM:
                destinationAddress = retrievePlatformAddress(recipient.getTaxId(), notification.getSender());
                break;
            case SPECIAL:
                destinationAddress = recipient.getDigitalDomicile();
                break;
            default:
                log.error("Specified addressSource {} does not exist for iun {} id {}", addressSource, notification.getIun(), recipient.getTaxId());
                throw new PnInternalException("Specified addressSource " + addressSource + " does not exist for iun " + notification.getIun() + " id " + recipient.getTaxId());
        }
        return destinationAddress;
    }

    private void handleCheckAddressAndSend(NotificationRecipient recipient, Notification notification, DigitalAddress destinationAddress, DigitalAddressSource2 addressSource) {
        String iun = notification.getIun();
        String taxId = recipient.getTaxId();

        if (destinationAddress != null) {
            log.debug("Destination address is available, send notification to external channel ");

            //Se l'indirizzo è disponibile, dunque valorizzato viene inviata la notifica ad external channel ...
            addAvailabilitySourceToTimeline(taxId, iun, addressSource, true);
            externalChannelService.sendDigitalNotification(notification, destinationAddress, recipient);

        } else {
            //... altrimenti si passa alla prossima workflow action
            log.debug("Destination address is not available, need to start next workflow action ");

            addAvailabilitySourceToTimeline(taxId, iun, addressSource, false);
            nextWorkFlowAction(iun, taxId);
        }
    }

    //TODO Questa logica è replicata in DIGITALWORFKLOW E CHOOSE DELIVERY PORTARLA A FATTOR COMUNE
    //Se il risultato è diverso da null allora i suoi campi sono diversi da null
    private DigitalAddress retrievePlatformAddress(String taxId, NotificationSender sender) {

        Optional<AddressBookEntry> addressBookEntryOpt = addressBook.getAddresses(taxId, sender);

        if (addressBookEntryOpt.isPresent()) {
            DigitalAddresses digitalAddress = addressBookEntryOpt.get().getDigitalAddresses(); //TODO Valutare se far ritornare un solo indirizzo all'addressbook e non una lista
            DigitalAddress platformAddress = digitalAddress.getPlatform();
            return platformAddress != null && platformAddress.getAddress() != null ? platformAddress : null;
        }
        return null;
    }

    private void addAvailabilitySourceToTimeline(String taxId, String iun, DigitalAddressSource2 source, boolean isAvailable) {
        timelineService.addTimelineElement(TimelineElement.builder()
                .category(TimelineElementCategory.GET_ADDRESS)
                .iun(iun)
                .details(GetAddressInfo.builder()
                        .taxId(taxId)
                        .source(source)
                        .isAvailable(isAvailable)
                        .build())
                .build());
    }

    private void sendRequestForGetGeneralAddress(String iun, String taxId) {
        String correlationId = iun + taxId + "_digital";
        log.debug("Start send request for get general address with correlationId {}", correlationId);

        publicRegistryService.sendRequestForGetAddress(iun, taxId, correlationId, DeliveryMode.DIGITAL, ContactPhase.SEND_ATTEMPT);
    }

    /**
     * Handle response to request for get special address. If address is present in response, send notification to this address else startNewWorkflow action.
     *
     * @param response Get special address response
     * @param iun      Notification unique identifier
     * @param taxId    User identifier
     */
    public void handleGeneralAddressResponse(PublicRegistryResponse response, String iun, String taxId) {

        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);

        handleCheckAddressAndSend(recipient, notification, response.getDigitalAddress(), DigitalAddressSource2.GENERAL);
    }


}
