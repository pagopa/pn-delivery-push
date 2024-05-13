package it.pagopa.pn.deliverypush.action.documentcreationresponsehandler;

import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.it.CommonTestConfiguration;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.AarCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.service.F24Service;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class AarCreationResponseHandlerTest extends CommonTestConfiguration {

    @MockBean
    NotificationService notificationService;
    @MockBean
    CourtesyMessageUtils courtesyMessageUtils;
    @Mock
    SchedulerService schedulerService;
    @MockBean
    TimelineService timelineService;
    @MockBean
    TimelineUtils timelineUtils;

    @MockBean
    F24Service f24Service;

    @Autowired
    AarCreationResponseHandler handler;

    @Test
    void testHandleAarCreationResponse(){

        ConsoleAppenderCustom.initializeLog();


        String iun="HETX-DAGU-VJWG-202306-Y-1";
        String timelineId="AAR_CREATION_REQUEST.IUN_HETX-DAGU-VJWG-202306-Y-1.RECINDEX_0";
        DocumentCreationTypeInt docType = DocumentCreationTypeInt.AAR;

        Mockito.when(timelineService.addTimelineElement(Mockito.any(), Mockito.any()))
            .thenReturn(true);

        AarCreationRequestDetailsInt aarCreationRequestDetailsInt = AarCreationRequestDetailsInt.builder()
            .recIndex(0)
            .aarKey("aarKey")
            .numberOfPages(1)
            .build();
        Optional<AarCreationRequestDetailsInt> value = Optional.of(aarCreationRequestDetailsInt);
        Mockito.when(timelineService.getTimelineElementDetails(iun, timelineId, AarCreationRequestDetailsInt.class))
            .thenReturn(value);
        DocumentCreationResponseActionDetails actionDetails = DocumentCreationResponseActionDetails.builder()
            .key("key").timelineId(timelineId).documentCreationType(docType)
            .build();

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(NotificationInt.builder().build());
        handler.handleAarCreationResponse(iun,0, actionDetails);

        //Then
        ConsoleAppenderCustom.checkWarningLogs("[{}] {} - File already present saving AAR fileKey={} iun={} recIndex={}");
    }
}
