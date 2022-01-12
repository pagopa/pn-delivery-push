package it.pagopa.pn.deliverypush.action2.it.analog;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action2.*;
import it.pagopa.pn.deliverypush.action2.it.AbstractWorkflowTestConfiguration;
import it.pagopa.pn.deliverypush.action2.it.TestUtils;
import it.pagopa.pn.deliverypush.action2.it.mockbean.*;
import it.pagopa.pn.deliverypush.action2.utils.*;
import it.pagopa.pn.deliverypush.actions.ExtChnEventUtils;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.impl.NotificationServiceImpl;
import it.pagopa.pn.deliverypush.service.impl.TimeLineServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StartWorkflowHandler.class,
        AnalogWorkflowHandler.class,
        ChooseDeliveryModeHandler.class,
        DigitalWorkFlowHandler.class,
        CompletionWorkFlowHandler.class,
        PublicRegistryResponseHandler.class,
        ExternalChannelResponseHandler.class,
        RefinementHandler.class,
        DigitalWorkFlowUtils.class,
        CourtesyMessageUtils.class,
        ExternalChannelUtils.class,
        CompletelyUnreachableUtils.class,
        ExtChnEventUtils.class,
        AnalogWorkflowUtils.class,
        TimelineUtils.class,
        PublicRegistryUtils.class,
        NotificationServiceImpl.class,
        TimeLineServiceImpl.class,
        PnDeliveryPushConfigs.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        SchedulerServiceMock.class,
        PublicRegistryMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        AnalogWorkflowTestTwo.SpringTestConfiguration.class
})
class AnalogWorkflowTestTwo {
    private static final List<Notification> listNotification = new ArrayList<>(getListNotification());
    private static final String taxId = listNotification.get(0).getRecipients().get(0).getTaxId();
    private static final List<AddressBookEntry> addressBookEntries = getListAddressBook(taxId);

    private static final Map<String, DigitalAddress> PUB_REGISTRY_DIGITAL = Collections.emptyMap();
    private static final Map<String, PhysicalAddress> PUB_REGISTRY_PHYSICAL = Collections.emptyMap();

    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {

        public SpringTestConfiguration() {
            super(listNotification, addressBookEntries, PUB_REGISTRY_DIGITAL, PUB_REGISTRY_PHYSICAL);
        }
    }

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;

    @Autowired
    private TimelineService timelineService;

    @Test
    void workflowTest() {
        /*Workflow analogico
           - Platform address vuoto (Ottenuto non valorizzando il digitalAddresses.getPlatform() dei digitalAddresses dell'address book Definito in LIST_ADDRESS_BOOK)
           - Special address vuoto (Ottenuto non valorizzando recipient.getDigitalDomicile() della notifica)
           - General address vuoto (Ottenuto inserendo testUtils.PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS nel taxId)
           
           - Pa physical address NON presente (Ottenuto NON valorizzando recipient.physicalAddress della notifica)
           - Public Registry indirizzo trovato ma restituisce un indirizzo che fallirà nell'invio di external channel e in tale invio l'indirizzo dall'investigazione
             ottenuto avrà esito positivo nel successivo invio
             (Ottenuto inserendo TestUtils.EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT e TestUtils.INVESTIGATION_ADDRESS_PRESENT_POSITIVE nel taxId del recipient della notifica) 
         */
        //Notifica utilizzata
        Notification notification = listNotification.get(0);
        String iun = notification.getIun();

        //Start del workflow
        startWorkflowHandler.startWorkflow(notification.getIun());

        //Viene verificato che non sia stato inviato alcun messaggio
        String eventId = TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .index(0)
                        .build());
        Optional<SendCourtesyMessageDetails> sendCourtesyMessageDetailsOpt = timelineService.getTimelineElement(iun, eventId, SendCourtesyMessageDetails.class);
        Assertions.assertFalse(sendCourtesyMessageDetailsOpt.isPresent());

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        checkGetAddress(iun, false, DigitalAddressSource.PLATFORM, ChooseDeliveryModeHandler.START_SENT_ATTEMPT_NUMBER);
        checkGetAddress(iun, false, DigitalAddressSource.SPECIAL, ChooseDeliveryModeHandler.START_SENT_ATTEMPT_NUMBER);
        checkGetAddress(iun, false, DigitalAddressSource.GENERAL, ChooseDeliveryModeHandler.START_SENT_ATTEMPT_NUMBER);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito da publicRegistry
        checkSendToExtChannel(iun, TestUtils.getPhysicalAddressWithTaxIdForPublicRegistry(taxId), 0);
        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dall'investigazione
        checkSendToExtChannel(iun, TestUtils.PHYSICAL_ADDRESS_OK, 1);

        //Viene verificato che il workflow abbia avuto successo
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.ANALOG_SUCCESS_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recipientId(taxId)
                                .build())).isPresent());

        //Viene verificato che sia avvenuto il perfezionamento
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recipientId(taxId)
                                .build())).isPresent());

    }

    private void checkSendToExtChannel(String iun, PhysicalAddress physicalAddress, int sendAttempt) {
        String eventIdFirstSend = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .index(sendAttempt)
                        .build());
        Optional<SendPaperDetails> sendPaperDetailsOpt = timelineService.getTimelineElement(iun, eventIdFirstSend, SendPaperDetails.class);
        Assertions.assertTrue(sendPaperDetailsOpt.isPresent());
        SendPaperDetails firstSendPaperDetails = sendPaperDetailsOpt.get();
        Assertions.assertEquals(physicalAddress, firstSendPaperDetails.getAddress());
    }

    private void checkGetAddress(String iun, Boolean isAvailable, DigitalAddressSource source, int sentAttempt) {
        String correlationId = TimelineEventId.GET_ADDRESS.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .source(source)
                        .index(sentAttempt)
                        .build());

        Optional<GetAddressInfo> getAddressInfoOpt = timelineService.getTimelineElement(iun, correlationId, GetAddressInfo.class);
        Assertions.assertTrue(getAddressInfoOpt.isPresent());
        Assertions.assertEquals(isAvailable, getAddressInfoOpt.get().isAvailable());
    }


    public static Collection<Notification> getListNotification() {
        Notification notification = Notification.builder()
                .iun("IUN01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .physicalCommunicationType(ServiceLevelType.SIMPLE_REGISTERED_LETTER)
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01_" + TestUtils.PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS + "_" + TestUtils.EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT + "_" + TestUtils.INVESTIGATION_ADDRESS_PRESENT_POSITIVE)
                                .denomination("Nome Cognome/Ragione Sociale")
                                .build()
                ))
                .documents(Arrays.asList(
                        NotificationAttachment.builder()
                                .ref(NotificationAttachment.Ref.builder()
                                        .key("key_doc00")
                                        .versionToken("v01_doc00")
                                        .build()
                                )
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .build(),
                        NotificationAttachment.builder()
                                .ref(NotificationAttachment.Ref.builder()
                                        .key("key_doc01")
                                        .versionToken("v01_doc01")
                                        .build()
                                )
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .build()
                ))
                .build();
        return Collections.singletonList(notification);
    }

    public static List<AddressBookEntry> getListAddressBook(String taxId) {
        AddressBookEntry addressBookEntry = AddressBookEntry.builder()
                .taxId(taxId)
                .build();
        return Collections.singletonList(addressBookEntry);
    }
}
