package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.RequestAccepted;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24.PnF24Client;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;

class F24ValidatorTest {

    @Mock
    private PnF24Client pnF24Client;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private TimelineService timelineService;

    private F24Validator f24Validator;


    @BeforeEach
    public void setup() {
        f24Validator = new F24Validator(pnF24Client, timelineService, timelineUtils);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void requestValidateF24() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotificationV2WithF24();

        RequestAccepted requestAccepted = new RequestAccepted();
        requestAccepted.setStatus("ok");
        requestAccepted.setDescription("desc");
        Mockito.when(pnF24Client.validate(any())).thenReturn(Mono.just(requestAccepted));

        final TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildValidateF24RequestTimelineElement(Mockito.eq(notification)))
                .thenReturn(timelineElement);

        //WHEN
        f24Validator.requestValidateF24(notification).block();

        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElement,notification);
    }
}
