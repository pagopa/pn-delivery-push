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
    public static final int MAX_ATTEMPT_NUMBER = 2;

    private TimelineService timelineService;
    private CompletionWorkFlow completionWorkFlow;
    private PublicRegistryHandler publicRegistrySender;
    private AddressBook addressBook;
    private ExternalChannel externalChannel;
    private NotificationDao notificationDao;
    private Scheduler scheduler;

    /**
     * Handle digital notification Workflow based on already made attempt
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    public void nextWorkFlowAction(String iun, String taxId) {
        AttemptAddressInfo nextAddressInfo = getNextAddressInfo(iun, taxId);

        if (nextAddressInfo.getAttemptNumberMade() < MAX_ATTEMPT_NUMBER) {
            switch (nextAddressInfo.getAttemptNumberMade()) {
                case 0:
                    //Start First attempt for this source
                    getAddressFromSourceAndSendNotification(iun, taxId, nextAddressInfo);
                    break;
                case 1:
                    //Start second attempt for this source
                    startOrScheduleNextWorkflow(iun, taxId, nextAddressInfo);
                    break;
                default:
                    //TODO Gestire errore
                    break;
            }
        } else {
            //Digital workflow is failed because all planned attempt have failed
            completionWorkFlow.completionDigitalWorkflow(taxId, iun, Instant.now(), EndWorkflowStatus.FAILURE);
        }
    }

    private void getAddressFromSourceAndSendNotification(String iun, String taxId, AttemptAddressInfo nextAddressInfo) {
        if (DigitalAddressSource2.SPECIAL.equals(nextAddressInfo.getAddressSource())) {
            sendRequestForGetSpecialAddress(iun, taxId); //special address need async call to get it
        } else {
            DigitalAddress digitalAddresses = getAddressFromAddressSource(nextAddressInfo.getAddressSource(), taxId, iun);
            //Get address from source and if is available send notification
            if (digitalAddresses != null && digitalAddresses.getAddress() != null) {
                sendNotificationToExternalChannel(iun, digitalAddresses, taxId);
            } else {
                //address is not available, start next workflow action
                nextWorkFlowAction(iun, taxId);
            }
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
    private void startOrScheduleNextWorkflow(String iun, String taxId, AttemptAddressInfo nextAddressInfo) {
        Instant schedulingDate = nextAddressInfo.lastAttemptDate.plus(7, ChronoUnit.DAYS);
        if (Instant.now().isAfter(schedulingDate)) {
            getAddressFromSourceAndSendNotification(iun, taxId, nextAddressInfo);
        } else {
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

    /**
     * return address from addressbook and save availability information for all address in timeline
     *
     * @return DigitalAddress for send notification
     */
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
        publicRegistrySender.sendRequestForGetAddress(iun, taxId, correlationId, DeliveryMode.DIGITAL, ContactPhase.SEND_ATTEMPT);
    }


    /**
     * Handle response to request for get special address. If address is present in response, send notification to this address else startNewWorkflow action.
     *
     * @param response Get special address response
     * @param iun      Notification unique identifier
     * @param taxId    User identifier
     */
    public void handleSpecialAddressResponse(PublicRegistryResponse response, String iun, String taxId) {
        if (response.getDigitalAddress() != null) {
            addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.SPECIAL, true);

            DigitalAddress digitalAddress = DigitalAddress.builder() //TODO Da cambiare
                    .address(response.getDigitalAddress())
                    .type(DigitalAddressType.PEC)
                    .build();
            sendNotificationToExternalChannel(iun, digitalAddress, taxId);
        } else {
            addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.SPECIAL, false);
            nextWorkFlowAction(iun, taxId);
        }
    }

    private AttemptAddressInfo getNextAddressInfo(String iun, String taxId) {
        //TODO Da rivedere i metodi utilizzati per filtrare ecc

        AttemptAddressInfo attemptAddressInfo;
        Set<TimelineElement> timeline = timelineService.getTimeline(iun);

        //Get last source tryed
        Optional<GetAddressInfo> lastAddressAttemptOpt = getLastAddressAttempt(taxId, timeline);

        if (lastAddressAttemptOpt.isPresent()) {
            GetAddressInfo lastAddressAttempt = lastAddressAttemptOpt.get();

            //Get next source to use from last used
            DigitalAddressSource2 nextAddressSource = getNextAddressSource(lastAddressAttempt.getSource());

            int attemptsMade = getAttemptsMadeForSource(taxId, timeline, nextAddressSource);

            attemptAddressInfo = AttemptAddressInfo.builder()
                    .addressSource(nextAddressSource)
                    .attemptNumberMade(attemptsMade)
                    .lastAttemptDate(lastAddressAttempt.getAttemptDate())
                    .build();
        } else {
            //TODO GESTIRE CASISTICA DI ERRORE, NON E' POSSIBILE ARRIVARE QUI
            throw new RuntimeException();
        }
        return attemptAddressInfo;
    }

    //Get last tryed source address from timeline. Attempt for source is ever added in timeline (both in case the address is available and if it's not available)
    private Optional<GetAddressInfo> getLastAddressAttempt(String taxId, Set<TimelineElement> timeline) {
        return timeline.stream()
                .filter(timelineElement -> filterLastAttemptDateInTimeline(timelineElement, taxId))
                .map(timelineElement -> (GetAddressInfo) timelineElement.getDetails()).min(Comparator.comparing(GetAddressInfo::getAttemptDate));
    }

    private boolean filterLastAttemptDateInTimeline(TimelineElement el, String taxId) {
        boolean availableAdressCategory = TimelineElementCategory.GET_ADDRESS.equals(el.getCategory());
        if (availableAdressCategory) {
            GetAddressInfo details = (GetAddressInfo) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId());
        }
        return false;
    }

    // Get attempts number made for passed source
    private int getAttemptsMadeForSource(String taxId, Set<TimelineElement> timeline, DigitalAddressSource2 nextAddressSource) {
        return (int) timeline.stream()
                .filter(timelineElement -> filterTimelineForTaxIdAndSource(timelineElement, taxId, nextAddressSource)).count();
    }

    private boolean filterTimelineForTaxIdAndSource(TimelineElement el, String taxId, DigitalAddressSource2 source) {
        boolean availableAddressCategory = TimelineElementCategory.GET_ADDRESS.equals(el.getCategory());
        if (availableAddressCategory) {
            GetAddressInfo details = (GetAddressInfo) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId()) && source.equals(details.getSource());
        }
        return false;
    }


    /**
     * Get next address source from passed source in this order: PLATFORM, GENERAL, SPECIAL
     *
     * @param addressSource
     * @return next address source
     */
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
        private int attemptNumberMade;
        private Instant lastAttemptDate;
    }
}
