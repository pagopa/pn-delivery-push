package it.pagopa.pn.deliverypush.actions;
/*
import java.time.Instant;
import java.util.Collections;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import it.pagopa.pn.api.dto.events.CommunicationType;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;

import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;

class ExtChnEventUtilsTest {	
	private ExtChnEventUtils utils;
    private PnDeliveryPushConfigs cfg;
    
    @BeforeEach
    public void setup() {
        utils = Mockito.mock(ExtChnEventUtils.class);
        cfg = Mockito.mock(PnDeliveryPushConfigs.class);
    }
    
    @Test
    void successBuildSendPaperRequest() {
    	//GIVEN
    	Notification notification = newNotification();
    	
    	Action action = Action.builder()
    					.iun( notification.getIun() )
    					.recipientIndex( 0 )
    					.notBefore( Instant.now() )
    					.type( ActionType.PEC_FAIL_SEND_PAPER )
    					.build();
    
    	//WHEN
    	utils.buildSendPaperRequest( action, notification, 
    			CommunicationType.RECIEVED_DELIVERY_NOTICE, ServiceLevelType.SIMPLE_REGISTERED_LETTER );
    	
    	//THEN
        ArgumentCaptor<Action> actionCapture = ArgumentCaptor.forClass(Action.class);
        ArgumentCaptor<Notification> notificationCapture = ArgumentCaptor.forClass(Notification.class);
        ArgumentCaptor<CommunicationType> communicationTypeCapture = ArgumentCaptor.forClass(CommunicationType.class);
        ArgumentCaptor<ServiceLevelType> serviceLevelTypeCapture = ArgumentCaptor.forClass(ServiceLevelType.class);


        Mockito.verify(utils).buildSendPaperRequest(actionCapture.capture(), notificationCapture.capture(), 
        		communicationTypeCapture.capture(), serviceLevelTypeCapture.capture() );
        
        Assertions.assertEquals(actionCapture.getValue(), action, "Different Action from the expected");
        Assertions.assertEquals(notificationCapture.getValue(), notification, "Different Notification from the expected");
        Assertions.assertEquals(CommunicationType.RECIEVED_DELIVERY_NOTICE, communicationTypeCapture.getValue(),"Different CommunicationType from the expected");
        Assertions.assertEquals(ServiceLevelType.SIMPLE_REGISTERED_LETTER, serviceLevelTypeCapture.getValue(), "Different ServiceLevelType from the expected");
    }
    
    @Test
    void successBuildSendPecRequest() {
    	//GIVEN
    	Notification notification = newNotification();
    	
    	Action action = Action.builder()
    					.iun( notification.getIun() )
    					.recipientIndex( 0 )
    					.notBefore( Instant.now() )
    					.type( ActionType.SEND_PEC )
    					.build();
    	
    	NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());
    	DigitalAddress address = notification.getRecipients().get(action.getRecipientIndex()).getDigitalDomicile();
    	
    	//WHEN
    	utils.buildSendPecRequest( action, notification, recipient, address );
    	
    	//THEN
        ArgumentCaptor<Action> actionCapture = ArgumentCaptor.forClass(Action.class);
        ArgumentCaptor<Notification> notificationCapture = ArgumentCaptor.forClass(Notification.class);
        ArgumentCaptor<NotificationRecipient> recipientCapture = ArgumentCaptor.forClass(NotificationRecipient.class);
        ArgumentCaptor<DigitalAddress> addressCapture = ArgumentCaptor.forClass(DigitalAddress.class);

        Mockito.verify(utils).buildSendPecRequest(actionCapture.capture(), notificationCapture.capture(),
        		recipientCapture.capture(), addressCapture.capture() );
        
        Assertions.assertEquals(actionCapture.getValue(), action, "Different Action from the expected");
        Assertions.assertEquals(notificationCapture.getValue(), notification, "Different Notification from the expected");
        Assertions.assertEquals(recipientCapture.getValue(), recipient, "Different NotificationRecipient from the expected");
        Assertions.assertEquals(addressCapture.getValue(), address, "Different DigitalAddress from the expected");
    }
    
    private Notification newNotification() {
        return Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddress.TypeEnum.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }
            
}


 */