package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.details.AnalogWorkflowTimeoutDetails;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.service.AnalogWorkflowTimeoutHandlerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.MessageHeaders;

import java.time.Instant;

import static org.mockito.Mockito.*;

public class AnalogWorkflowTimeoutHandlerTest {

    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private AnalogWorkflowTimeoutHandlerService analogWorkflowTimeoutHandlerService;
    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private AnalogWorkflowTimeoutHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new AnalogWorkflowTimeoutHandler(timelineUtils, analogWorkflowTimeoutHandlerService);
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

        verify(analogWorkflowTimeoutHandlerService, times(1))
                .handleAnalogWorkflowTimeout(iun, timelineId, recipientIndex, details, notBefore);
    }

    @Test
    void testGetSupportedEventType() {
        AnalogWorkflowTimeoutHandler handler = new AnalogWorkflowTimeoutHandler(timelineUtils, analogWorkflowTimeoutHandlerService);
        Assertions.assertEquals(SupportedEventType.ANALOG_WORKFLOW_NO_FEEDBACK_TIMEOUT, handler.getSupportedEventType());
    }


}
