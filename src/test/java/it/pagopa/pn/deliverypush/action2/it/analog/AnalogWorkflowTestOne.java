package it.pagopa.pn.deliverypush.action2.it.analog;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
        AnalogWorkflowTestOne.SpringTestConfiguration.class
})
class AnalogWorkflowTestOne {
    private static final List<Notification> listNotification = new ArrayList<>(getListNotification());
    private static final String taxId = listNotification.get(0).getRecipients().get(0).getTaxId();
    private static final List<AddressBookEntry> addressBookEntries = getListAddressBook(taxId);

    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {

        public SpringTestConfiguration() {
            super(listNotification, addressBookEntries);
        }
    }

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;

    @Autowired
    private TimelineService timelineService;

    @SpyBean
    private ExternalChannelMock externalChannelMock;

    @Test
    void workflowTest() {
        /*Workflow analogico
           - Platform address vuoto (Ottenuto non valorizzando il digitalAddresses.getPlatform() dei digitaladdresses dell'address book Definito in LIST_ADDRESS_BOOK)
           - Special address vuoto (Ottenuto non valorizzando recipient.getDigitalDomicile() della notifica)
           - General address vuoto (Ottenuto inserendo testUtils.PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS nel taxId)
           
           - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando CourtesyAddress getListAddressBook())
           - Pa physical address presente con indirizzo che porta al fallimento della notifica da externalChannel
              (Ottenuto valorizzando recipient.physicalAddress della notifica inserendo TestUtils.EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT)
           - Public Registry Indirizzo non trovato KO (Ottenuto inserendo TestUtils.PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS nel taxId)
           - Indirizzo investigazione presente ma con successivo fallimento in invio (Ottenuto inserendo INVESTIGATION_ADDRESS_PRESENT_FAILURE PhysicalAddress.address)
         */

        //Notifica utilizzata
        Notification notification = listNotification.get(0);
        String iun = notification.getIun();
        NotificationRecipient recipient = notification.getRecipients().get(0);

        //Start del workflow
        startWorkflowHandler.startWorkflow(notification.getIun());

        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        List<DigitalAddress> courtesyAddresses = addressBookEntries.get(0).getCourtesyAddresses();

        int index = 0;
        for (DigitalAddress digitalAddress : courtesyAddresses) {
            String eventId = TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(
                    EventId.builder()
                            .iun(iun)
                            .recipientId(taxId)
                            .index(index)
                            .build());
            Optional<SendCourtesyMessageDetails> sendCourtesyMessageDetailsOpt = timelineService.getTimelineElement(iun, eventId, SendCourtesyMessageDetails.class);

            Assertions.assertTrue(sendCourtesyMessageDetailsOpt.isPresent());
            SendCourtesyMessageDetails sendCourtesyMessageDetails = sendCourtesyMessageDetailsOpt.get();
            Assertions.assertEquals(digitalAddress, sendCourtesyMessageDetails.getAddress());
            index++;
        }
        //Viene verificato l'effettivo invio del messaggio di cortesia verso external channel
        Mockito.verify(externalChannelMock, Mockito.times(courtesyAddresses.size())).sendNotification(Mockito.any(PnExtChnEmailEvent.class));

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        checkGetAddress(iun, false, DigitalAddressSource.PLATFORM, ChooseDeliveryModeHandler.START_SENT_ATTEMPT_NUMBER);
        checkGetAddress(iun, false, DigitalAddressSource.SPECIAL, ChooseDeliveryModeHandler.START_SENT_ATTEMPT_NUMBER);
        checkGetAddress(iun, false, DigitalAddressSource.GENERAL, ChooseDeliveryModeHandler.START_SENT_ATTEMPT_NUMBER);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla pa
        checkSendToExtChannel(iun, recipient.getPhysicalAddress(), 0);
        //Viene verificata la presenza del secondo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dal postino
        checkSendToExtChannel(iun, TestUtils.PHYSICAL_ADDRESS_FAILURE_BOTH, 1);

        //Viene verificato l'effettivo invio delle due notifiche verso externalChannel
        Mockito.verify(externalChannelMock, Mockito.times(2)).sendNotification(Mockito.any(PnExtChnPaperEvent.class));

        //Viene verificato che il workflow è fallito
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recipientId(taxId)
                                .build())).isPresent());

        //Viene verificato che il destinatario risulti completamente irraggiungibile
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
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

    private boolean checkCourtesyMessage(TimelineElement el, String taxId) {
        boolean availableAddressCategory = TimelineElementCategory.SEND_COURTESY_MESSAGE.equals(el.getCategory());
        if (availableAddressCategory) {
            SendCourtesyMessageDetails details = (SendCourtesyMessageDetails) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId());
        }
        return false;
    }

    private boolean checkCompletelyUnreachable(TimelineElement el, String taxId) {
        boolean availableAddressCategory = TimelineElementCategory.COMPLETELY_UNREACHABLE.equals(el.getCategory());
        if (availableAddressCategory) {
            CompletlyUnreachableDetails details = (CompletlyUnreachableDetails) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId());
        }
        return false;
    }

    public static Collection<Notification> getListNotification() {
        Notification notification = Notification.builder()
                .iun("IUN01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .physicalCommunicationType(ServiceLevelType.SIMPLE_REGISTERED_LETTER)
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01_" + TestUtils.PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS + "_" + TestUtils.PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS)
                                .denomination("Nome Cognome/Ragione Sociale")
                                .physicalAddress(PhysicalAddress.builder()
                                        .at("Presso")
                                        .address("Via di casa sua - " + TestUtils.EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT + "_" + TestUtils.INVESTIGATION_ADDRESS_PRESENT_FAILURE)
                                        .zip("00100")
                                        .municipality("Roma")
                                        .province("RM")
                                        .foreignState("IT")
                                        .addressDetails("Scala A")
                                        .build())
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
                .courtesyAddresses(Collections.singletonList((DigitalAddress.builder()
                        .address("Via nuova 26")
                        .type(DigitalAddressType.PEC)
                        .build())))
                .build();
        return Collections.singletonList(addressBookEntry);
    }
}
