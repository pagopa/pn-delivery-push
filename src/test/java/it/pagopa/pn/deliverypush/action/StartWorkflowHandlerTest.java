package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

class StartWorkflowHandlerTest {
    @Mock
    private NotificationValidationScheduler notificationValidationScheduler;
    
    private StartWorkflowHandler handler;
    
    @BeforeEach
    public void setup() {
        handler = new StartWorkflowHandler(notificationValidationScheduler);
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void startWorkflowOk() {
        //GIVEN
        String iun = "iun01";
        
        //WHEN
        handler.startWorkflow(iun);
        
        //THEN
        Mockito.verify(notificationValidationScheduler).scheduleNotificationValidation(iun);
    }

}