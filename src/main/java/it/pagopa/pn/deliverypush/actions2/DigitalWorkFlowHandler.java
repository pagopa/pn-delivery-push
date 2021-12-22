package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.addressbook.DigitalAddresses;
import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource2;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import lombok.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class DigitalWorkFlowHandler {
    private TimelineService timelineService;
    private CompletionWorkFlow completionWorkFlow;
    private PublicRegistryHandler publicRegistrySender;
    private AddressBook addressBook;
    private ExternalChannel externalChannel;
    private NotificationDao notificationDao;
    private Scheduler scheduler;

    public void nextWorkFlowAction(String iun, String taxId) {
        AttemptAddressInfo nextAddressInfo = getNextAddressInfo(iun, taxId);

        if (nextAddressInfo.getNumberOfAttemptMade() < 2) {
            switch (nextAddressInfo.getNumberOfAttemptMade()) {
                case 0:
                    handleFirstAttempt(iun, taxId, nextAddressInfo);
                    break;
                case 1:
                    handleSecondAttempt(iun, taxId, nextAddressInfo);
                    break;
                default:
                    //TODO Gestire errore
                    break;
            }
        } else {
            //Sono stati effettuati i 2 tentativi previsti senza successo
            completionWorkFlow.endOfDigitalWorkflow(taxId, iun, Instant.now(), EndWorkflowStatus.FAILURE);
        }
    }

    private void handleFirstAttempt(String iun, String taxId, AttemptAddressInfo nextAddressInfo) {
        if (DigitalAddressSource2.SPECIAL.equals(nextAddressInfo.getAddressSource())) {
            sendRequestForGetSpecialAddress(iun, taxId);
        } else {
            DigitalAddress digitalAddresses = getAddressFromAddressSource(nextAddressInfo.getAddressSource(), taxId, iun);
            if (digitalAddresses != null && digitalAddresses.getAddress() != null) {
                sendNotificationToExternalChannel(iun, digitalAddresses, taxId);
            } else {
                nextWorkFlowAction(iun, taxId);
            }
        }
    }

    private void handleSecondAttempt(String iun, String taxId, AttemptAddressInfo nextAddressInfo) {
        Instant schedulingDate = nextAddressInfo.lastAttemptDate.plus(7, ChronoUnit.DAYS);
        //Se sono passati già 7 giorni dall'ultimo tentativo effettuato, ad esempio perchè già è stato effettuato lo scheduling dei 7 gg per un precedente indirizzo
        if (Instant.now().isAfter(schedulingDate)) {
            nextWorkFlowAction(iun, taxId);
        } else {
            //non sono passati i 7 giorni previsti dalla schedulazione bisogna quindi schedulare l'evento alla data prevista
            scheduler.schedulEvent(schedulingDate, ActionType.DIGITAL_WORKFLOW);
        }
    }

    private void sendNotificationToExternalChannel(String iun, DigitalAddress digitalAddress, String taxId) {
        Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);
        if (optNotification.isPresent()) {
            Notification notification = optNotification.get();
            NotificationRecipient recipient = null; //TODO Ottiene il recipient dalla notifica

            externalChannel.sendDigitalNotification(notification, digitalAddress, iun, recipient);
        } else {
            //TODO Gestione casisitca errore
        }
    }

    private DigitalAddress getAddressFromAddressSource(DigitalAddressSource2 addressSource, String taxId, String iun) {
        AtomicReference<DigitalAddress> digitalAddress = new AtomicReference<>();

        addressBook.getAddresses(taxId)
                .ifPresent(addressBookItem -> {
                    DigitalAddresses digitalAddresses = addressBookItem.getDigitalAddresses();
                    switch (addressSource) {
                        case PLATFORM:
                            if (isAddressAvailable(digitalAddresses, digitalAddresses.getPlatform())) {
                                digitalAddress.set(digitalAddresses.getPlatform());
                                addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.PLATFORM, true);
                            } else {
                                addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.PLATFORM, false);
                            }
                            break;
                        case GENERAL:
                            if (isAddressAvailable(digitalAddresses, digitalAddresses.getGeneral())) {
                                digitalAddress.set(digitalAddresses.getGeneral());
                                addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.GENERAL, true);
                            } else {
                                addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.GENERAL, false);
                            }
                            break;
                        default:
                            //TODO GESTIONE ERRORE
                            break;
                    }
                });

        return digitalAddress.get();
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

    private boolean isAddressAvailable(DigitalAddresses digitalAddresses, DigitalAddress platform) {
        return digitalAddresses != null && platform != null && platform.getAddress() != null;
    }

    private void sendRequestForGetSpecialAddress(String iun, String taxId) {
        String correlationId = iun + taxId + "_digital";
        publicRegistrySender.sendNotification(iun, taxId, correlationId, DeliveryMode.DIGITAL, ContactPhase.SEND_ATTEMPT);
    }

    public void handleSpecialAddressResponse(PublicRegistryResponse response, String iun, String taxId) {
        if (response.getDigitalAddress() != null) {
            DigitalAddress digitalAddress = DigitalAddress.builder()
                    .address(response.getDigitalAddress())
                    .type(DigitalAddressType.PEC)
                    .build();
            sendNotificationToExternalChannel(iun, digitalAddress, taxId);
        } else {
            nextWorkFlowAction(iun, taxId);
        }
    }

    private AttemptAddressInfo getNextAddressInfo(String iun, String taxId) {
        AttemptAddressInfo attemptAddressInfo;
        Set<TimelineElement> timeline = timelineService.getTimeline(iun);

        Optional<GetAddressInfo> lastAddressAttemptOpt = timeline.stream()
                .filter(timelineElement -> filterLastAttemptDateInTimeline(timelineElement, taxId))
                .map(timelineElement -> (GetAddressInfo) timelineElement.getDetails()).min(Comparator.comparing(GetAddressInfo::getAttemptDate));

        //Ottengo l'indirizzo nuovo

        if (lastAddressAttemptOpt.isPresent()) {
            GetAddressInfo lastAddressAttempt = lastAddressAttemptOpt.get();
            DigitalAddressSource2 nextAddressSource = getNextAddressSource(lastAddressAttempt.getSource());
            int attemptMade = (int) timeline.stream()
                    .filter(timelineElement -> filterTimelineForTaxIdAndSource(timelineElement, taxId, nextAddressSource)).count();
            attemptAddressInfo = AttemptAddressInfo.builder()
                    .addressSource(nextAddressSource)
                    .numberOfAttemptMade(attemptMade)
                    .lastAttemptDate(lastAddressAttempt.getAttemptDate())
                    .build();
        } else {
            //TODO GESTIRE CASISTICA DI ERRORE, NON E' POSSIBILE ARRIVARE QUI
            throw new RuntimeException();
        }
        return attemptAddressInfo;
    }

    private boolean filterLastAttemptDateInTimeline(TimelineElement el, String taxId) {
        boolean availableAdressCategory = TimelineElementCategory.GET_ADDRESS.equals(el.getCategory());
        if (availableAdressCategory) {
            GetAddressInfo details = (GetAddressInfo) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId());
        }
        return false;
    }

    private boolean filterTimelineForTaxIdAndSource(TimelineElement el, String taxId, DigitalAddressSource2 source) {
        boolean availableAddressCategory = TimelineElementCategory.GET_ADDRESS.equals(el.getCategory());
        if (availableAddressCategory) {
            GetAddressInfo details = (GetAddressInfo) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId()) && source.equals(details.getSource());
        }
        return false;
    }

    public DigitalAddressSource2 getNextAddressSource(DigitalAddressSource2 addressSource) {
        switch (addressSource) {
            case PLATFORM:
                return DigitalAddressSource2.GENERAL;
            case GENERAL:
                return DigitalAddressSource2.SPECIAL;
            case SPECIAL:
                return DigitalAddressSource2.PLATFORM;
            default:
                //TODO GESTIONE ERRORE
                throw new RuntimeException();
        }
    }


    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Builder(toBuilder = true)
    @EqualsAndHashCode
    @ToString
    public static class AttemptAddressInfo {
        private DigitalAddressSource2 addressSource;
        private int numberOfAttemptMade;
        private Instant lastAttemptDate;
    }
}
