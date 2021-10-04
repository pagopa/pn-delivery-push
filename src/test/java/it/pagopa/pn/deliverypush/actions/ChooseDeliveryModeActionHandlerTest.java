package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.addressbook.DigitalAddresses;
import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ChooseDeliveryModeActionHandlerTest {
    private AddressBook addressBook;
    private TimelineDao timelineDao;
    private ActionsPool actionsPool;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private ChooseDeliveryModeActionHandler handler;
    private MomProducer<PnExtChnEmailEvent> emailRequestProducer;

    @BeforeEach
    void setup() {
        emailRequestProducer = Mockito.mock( MomProducer.class );
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        addressBook = Mockito.mock(AddressBook.class);
        timelineDao = Mockito.mock(TimelineDao.class);
        actionsPool = Mockito.mock(ActionsPool.class);
        handler = new ChooseDeliveryModeActionHandler(
                timelineDao,
                addressBook,
                actionsPool,
                pnDeliveryPushConfigs,
                emailRequestProducer );
        TimeParams times = new TimeParams();
        times.setRecipientViewMaxTime(Duration.ZERO);
        times.setSecondAttemptWaitingTime(Duration.ZERO);
        times.setIntervalBetweenNotificationAndMessageReceived(Duration.ZERO);
        times.setWaitingForNextAction(Duration.ZERO);
        times.setTimeBetweenExtChReceptionAndMessageProcessed(Duration.ZERO);
        times.setWaitingResponseFromFirstAddress(Duration.ZERO);
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
    }

    @Test
    void successHandleActionTest() {
        //Given
        Action nextAction = Action.builder()
                .iun("Test_iun01")
                .recipientIndex(0)
                .type(ActionType.SEND_PEC)
                .actionId("Test_iun01_send_pec_rec0_null_nnull")
                .build();

        String actionId = nextAction.getType().buildActionId(nextAction);
        nextAction = nextAction.toBuilder().actionId(actionId).build();

        Notification notification = newNotificationWithoutPayments();

        List<DigitalAddress> courtesyAddresses = Arrays.asList(
                DigitalAddress.builder()
                        .type(DigitalAddressType.EMAIL)
                        .address("nome1.cognome1@develop1.it")
                        .build(),
                DigitalAddress.builder()
                        .type(DigitalAddressType.EMAIL)
                        .address("nome2.cognome2@develop2.it")
                        .build()
        );
        Mockito.when( addressBook.getAddresses( Mockito.anyString()) ).thenReturn(Optional.of(
           AddressBookEntry.builder()
                   .digitalAddresses( DigitalAddresses.builder()
                        .general( DigitalAddress.builder()
                                .type( DigitalAddressType.PEC )
                                .address( "a@pec")
                                .build())
                        .platform( DigitalAddress.builder()
                                .type( DigitalAddressType.PEC )
                                .address( "b@pec")
                                .build())
                        .build()
                   )
                   .courtesyAddresses(courtesyAddresses)
                   .build()
        ));
        int numberOfAddresses = courtesyAddresses.size();
        //doNothing().when( emailRequestProducer ).push( Mockito.any( PnExtChnEmailEvent.class ) );



        //When
        handler.handleAction(nextAction, notification);

        //Then
        ArgumentCaptor<PnExtChnEmailEvent> emailEventCaptor = ArgumentCaptor.forClass(PnExtChnEmailEvent.class);
        verify( emailRequestProducer, times( numberOfAddresses ) ).push( emailEventCaptor.capture() );

        List<PnExtChnEmailEvent> events = emailEventCaptor.getAllValues();
        for (int idx = 0; idx < numberOfAddresses; idx ++) {
            assertEquals( courtesyAddresses.get( idx).getAddress(), events.get( idx ).getPayload().getEmailAddress() );
        }
        ArgumentCaptor<String> taxIdCapture = ArgumentCaptor.forClass(String.class);
        Mockito.verify(addressBook).getAddresses(taxIdCapture.capture());

        Assertions.assertEquals(notification.getRecipients().get(0).getTaxId(), taxIdCapture.getValue());

    }

    @Test
    void successGetActionType() {
        //When
        ActionType actionType = handler.getActionType();
        //Then
        Assertions.assertEquals(ActionType.CHOOSE_DELIVERY_MODE, actionType, "Different Action Type");
    }

    private Notification newNotificationWithoutPayments() {
        return Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }

}
