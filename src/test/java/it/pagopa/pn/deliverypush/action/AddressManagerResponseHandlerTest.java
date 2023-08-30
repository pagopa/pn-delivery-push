package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.NormalizeItemsResult;
import it.pagopa.pn.deliverypush.middleware.responsehandler.AddressManagerResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class AddressManagerResponseHandlerTest {
    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private NotificationValidationActionHandler notificationValidationActionHandler;

    private AddressManagerResponseHandler handler;

    @BeforeEach
    public void setup() {
        handler = new AddressManagerResponseHandler(notificationValidationActionHandler, timelineUtils);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void handleResponseCancelled() {
        //GIVEN
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(true);

        NormalizeItemsResult normalizeItemsResult = new NormalizeItemsResult();
        //WHEN
        handler.handleResponseReceived(normalizeItemsResult);

        //THEN
        Mockito.verify(notificationValidationActionHandler, Mockito.never()).handleValidateAndNormalizeAddressResponse(Mockito.anyString(), Mockito.any());
    }
}
