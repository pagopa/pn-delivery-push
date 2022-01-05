package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.events.CommunicationType;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

class SendPaperActionHandlerTest {

    public static final String DIRECT_ACCESS_URL_TEMPLATE = "http://localhost:8090/direct-access.html?token=%s";
    private MomProducer<PnExtChnPaperEvent> paperRequestProducer;
    private TimelineDao timelineDao;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private SendPaperActionHandler handler;
    private AddressBook addressBook;

    @BeforeEach
    void setup() {
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        paperRequestProducer = Mockito.mock(MomProducer.class);
        timelineDao = Mockito.mock(TimelineDao.class);
        addressBook = Mockito.mock(AddressBook.class);
        handler = new SendPaperActionHandler(
                timelineDao,
                null,
                pnDeliveryPushConfigs,
                paperRequestProducer,
                new ExtChnEventUtils(pnDeliveryPushConfigs),
                addressBook);
        TimeParams times = new TimeParams();
        times.setRecipientViewMaxTimeForAnalog(Duration.ZERO);
        times.setSecondAttemptWaitingTime(Duration.ZERO);
        times.setIntervalBetweenNotificationAndMessageReceived(Duration.ZERO);
        times.setWaitingForNextAction(Duration.ZERO);
        times.setTimeBetweenExtChReceptionAndMessageProcessed(Duration.ZERO);
        times.setWaitingResponseFromFirstAddress(Duration.ZERO);
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        PnDeliveryPushConfigs.Webapp webAppCgf = new PnDeliveryPushConfigs.Webapp();
        webAppCgf.setDirectAccessUrlTemplate(DIRECT_ACCESS_URL_TEMPLATE);
        Mockito.when(pnDeliveryPushConfigs.getWebapp()).thenReturn(webAppCgf);
    }

    @Test
    void successHandleAction() {

        //Given
        // Action primo tentativo
        Action inputAction = Action.builder()
                .type(ActionType.SEND_PAPER)
                .iun("test_iun")
                .retryNumber(1)
                .notBefore(Instant.now())
                .recipientIndex(0)
                .actionId("test_iun_send_paper_rec0_n1")
                .build();

        Notification notification = newNotificationWithoutPayments();

        final PhysicalAddress physicalAddress = PhysicalAddress.builder()
                .at("presso")
                .address("via di casa sua")
                .addressDetails("scala A")
                .zip("00100")
                .municipality("Roma")
                .province("RM")
                .foreignState("IT")
                .build();

        Mockito.when(timelineDao.getTimelineElement(
                        Mockito.anyString(),
                        Mockito.anyString()))
                .thenReturn(Optional.of(TimelineElement.builder()
                        .details(NotificationPathChooseDetails.builder()
                                .physicalAddress(physicalAddress)
                                .build())
                        .build()));


        //When
        handler.handleAction(inputAction, notification);


        //Then
        ArgumentCaptor<PnExtChnPaperEvent> paperEventArg = ArgumentCaptor.forClass(PnExtChnPaperEvent.class);
        Mockito.verify(paperRequestProducer).push(paperEventArg.capture());
        final PnExtChnPaperEvent value = paperEventArg.getValue();
        Assertions.assertEquals(CommunicationType.RECIEVED_DELIVERY_NOTICE, value.getPayload().getCommunicationType());
        Assertions.assertEquals(true, value.getPayload().isInvestigation());
        Assertions.assertEquals(physicalAddress, value.getPayload().getDestinationAddress());

        ArgumentCaptor<TimelineElement> timeLineArg = ArgumentCaptor.forClass(TimelineElement.class);
        Mockito.verify(timelineDao).addTimelineElement(timeLineArg.capture());
        final TimelineElement timeLineArgValue = timeLineArg.getValue();
        Assertions.assertEquals(TimelineElementCategory.SEND_ANALOG_DOMICILE, (timeLineArgValue.getCategory()));
        final SendPaperDetails details = (SendPaperDetails) timeLineArgValue.getDetails();
        Assertions.assertEquals(true, details.isInvestigation());

    }

    @Test
    void successHandleSecondAttemptAction() {

        //Given
        final PhysicalAddress physicalAddress = PhysicalAddress.builder()
                .at("presso")
                .address("via di casa sua")
                .addressDetails("scala A")
                .zip("00100")
                .municipality("Roma")
                .province("RM")
                .foreignState("IT")
                .build();

        // Action secondo tentativo
        Action inputAction = Action.builder()
                .type(ActionType.SEND_PAPER)
                .iun("test_iun")
                .retryNumber(2)
                .notBefore(Instant.now())
                .recipientIndex(0)
                .newPhysicalAddress(physicalAddress)
                .attachmentKeys(Collections.singletonList("letter_template.pdf"))
                .actionId("test_iun_send_paper_rec0_n2")
                .build();

        Notification notification = newNotificationWithoutPayments();


        Mockito.when(timelineDao.getTimelineElement(
                        Mockito.anyString(),
                        Mockito.anyString()))
                .thenReturn(Optional.of(TimelineElement.builder()
                        .details(new SendPaperFeedbackDetails(
                                SendPaperDetails.builder()
                                        .build(),
                                inputAction.getNewPhysicalAddress(),
                                inputAction.getAttachmentKeys(),
                                Collections.singletonList("")
                        )).build()));

        Mockito.when(addressBook.getAddresses(
                        Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.of(AddressBookEntry.builder()
                        .residentialAddress(physicalAddress)
                        .build()));


        //When
        handler.handleAction(inputAction, notification);


        //Then
        ArgumentCaptor<PnExtChnPaperEvent> paperEventArg = ArgumentCaptor.forClass(PnExtChnPaperEvent.class);
        Mockito.verify(paperRequestProducer).push(paperEventArg.capture());
        final PnExtChnPaperEvent value = paperEventArg.getValue();
        Assertions.assertEquals(CommunicationType.RECIEVED_DELIVERY_NOTICE, value.getPayload().getCommunicationType());
        Assertions.assertEquals(false, value.getPayload().isInvestigation());
        Assertions.assertEquals(physicalAddress, value.getPayload().getDestinationAddress());

        ArgumentCaptor<TimelineElement> timeLineArg = ArgumentCaptor.forClass(TimelineElement.class);
        Mockito.verify(timelineDao).addTimelineElement(timeLineArg.capture());
        final TimelineElement timeLineArgValue = timeLineArg.getValue();
        Assertions.assertEquals(TimelineElementCategory.SEND_ANALOG_DOMICILE, (timeLineArgValue.getCategory()));
        final SendPaperDetails details = (SendPaperDetails) timeLineArgValue.getDetails();
        Assertions.assertEquals(false, details.isInvestigation());

    }

    private Notification newNotificationWithoutPayments() {
        return Notification.builder()
                .iun("test_iun")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .physicalAddress(PhysicalAddress.builder()
                                        .at("presso")
                                        .address("via di casa sua")
                                        .addressDetails("scala A")
                                        .zip("00100")
                                        .municipality("Roma")
                                        .province("RM")
                                        .foreignState("IT")
                                        .build())
                                .build()
                ))
                .build();
    }

    @Test
    void successGetActionType() {
        //When
        ActionType actionType = handler.getActionType();
        //Then
        Assertions.assertEquals(ActionType.SEND_PAPER, actionType, "Different Action Type");
    }
}
