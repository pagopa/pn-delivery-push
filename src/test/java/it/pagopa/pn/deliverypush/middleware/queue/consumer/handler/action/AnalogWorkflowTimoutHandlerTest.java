package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.details.AnalogWorkflowTimeoutDetails;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.service.impl.AnalogWorkflowTimoutHandlerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.MessageHeaders;


import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class AnalogWorkflowTimoutHandlerTest {

    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private AnalogWorkflowTimoutHandlerServiceImpl analogWorkflowTimoutHandlerService;
    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private AnalogWorkflowTimoutHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new AnalogWorkflowTimoutHandler(timelineUtils, analogWorkflowTimoutHandlerService);
    }

    @Test
    void testHandleCallsService() {
        String iun = "iun";
        String timelineId = "timelineId";
        int recipientIndex = 1;
        Instant notBefore = Instant.now();
        AnalogWorkflowTimeoutDetails details = mock(AnalogWorkflowTimeoutDetails.class);

        Action action = mock(Action.class);
        when(action.getIun()).thenReturn(iun);
        when(action.getTimelineId()).thenReturn(timelineId);
        when(action.getRecipientIndex()).thenReturn(recipientIndex);
        when(action.getDetails()).thenReturn(details);
        when(action.getNotBefore()).thenReturn(notBefore);

        handler.handle(action, headers);

        verify(analogWorkflowTimoutHandlerService, times(1))
                .handleAnalogWorkflowTimeout(iun, timelineId, recipientIndex, details, notBefore);
    }

    @Test
    void testGetSupportedEventType() {
        AnalogWorkflowTimoutHandler handler = new AnalogWorkflowTimoutHandler(timelineUtils, analogWorkflowTimoutHandlerService);
        assertEquals(SupportedEventType.ANALOG_WORKFLOW_NO_FEEDBACK_TIMEOUT, handler.getSupportedEventType());
    }


}
