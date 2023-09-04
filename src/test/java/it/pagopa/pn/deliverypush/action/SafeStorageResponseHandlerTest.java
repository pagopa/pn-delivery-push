package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.middleware.responsehandler.SafeStorageResponseHandler;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class SafeStorageResponseHandlerTest {
    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private DocumentCreationRequestService documentCreationRequestService;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private FileDownloadResponse fileDownloadResponse;

    private SafeStorageResponseHandler handler;


    @BeforeEach
    public void setup() {
        handler = new SafeStorageResponseHandler(documentCreationRequestService, schedulerService, timelineUtils);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void handleResponseCancelled() {
        String iun = "IUN-handleResponseCancelled";
        String fileKey = "fileKey";
        //GIVEN
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(iun)).thenReturn(true);
        Mockito.when(fileDownloadResponse.getKey()).thenReturn(fileKey);

        DocumentCreationRequest documentCreationRequest = DocumentCreationRequest.builder().iun(iun).build();
        Mockito.when(documentCreationRequestService.getDocumentCreationRequest(Mockito.anyString())).thenReturn(Optional.of(documentCreationRequest));
        //WHEN
        handler.handleSafeStorageResponse(fileDownloadResponse);

        //THEN
        //schedulerService.scheduleEvent(request.getIun(), request.getRecIndex(), schedulingDate, ActionType.DOCUMENT_CREATION_RESPONSE, request.getTimelineId(), details);
        Mockito.verify(schedulerService, Mockito.never()).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

}
