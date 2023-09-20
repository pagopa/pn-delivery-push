package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeItemsResultInt;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeResultInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnValidationNotValidAddressException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.RequestAccepted;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.ValidateF24Request;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24.PnF24Client;
import it.pagopa.pn.deliverypush.service.AddressManagerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

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
        ValidateF24Request validateF24Request = new ValidateF24Request();
        validateF24Request.setSetId(notification.getIun());

        RequestAccepted requestAccepted = new RequestAccepted();
        requestAccepted.setStatus("ok");
        requestAccepted.setDescription("desc");
        Mockito.when(pnF24Client.validate(any())).thenReturn(Mono.just(requestAccepted));

        final TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildValidateF24TimelineElement(Mockito.eq(notification), Mockito.anyString()))
                .thenReturn(timelineElement);

        //WHEN
        f24Validator.requestValidateF24(notification, validateF24Request).block();

        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElement,notification);
    }
}
