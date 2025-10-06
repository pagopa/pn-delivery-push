package it.pagopa.pn.deliverypush.action.documentcreationresponsehandler;

import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.it.CommonTestConfiguration;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.AarCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.service.F24Service;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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
    @MockBean
    private FeatureEnabledUtils featureEnabledUtils;

    @Test
    void testHandleAarCreationResponse(){

        ConsoleAppenderCustom.initializeLog();
        Instant notificationSentAt = Instant.now();

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

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(buildNotificationInt(notificationSentAt));
        Mockito.when(featureEnabledUtils.isSendCourtesyAtAARGenerationEnabled(notificationSentAt)).thenReturn(true);
        handler.handleAarCreationResponse(iun,0, actionDetails);

        Mockito.verify(courtesyMessageUtils, times(1))
                .checkAddressesAndSendCourtesyMessage(buildNotificationInt(notificationSentAt), 0, null);

        //Then
        ConsoleAppenderCustom.checkWarningLogs("[{}] {} - File already present saving AAR fileKey={} iun={} recIndex={}");
    }

    @Test
    void testHandleAarCreationResponse_FeatureFlag_SendCourtesyAtAARGenerationEnabled_false(){

        ConsoleAppenderCustom.initializeLog();
        Instant notificationSentAt = Instant.now();

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

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(buildNotificationInt(notificationSentAt));
        Mockito.when(featureEnabledUtils.isSendCourtesyAtAARGenerationEnabled(notificationSentAt)).thenReturn(false);
        handler.handleAarCreationResponse(iun,0, actionDetails);

        Mockito.verify(courtesyMessageUtils, never())
                .checkAddressesAndSendCourtesyMessage(buildNotificationInt(notificationSentAt), 0, null);
        //Then
        ConsoleAppenderCustom.checkWarningLogs("[{}] {} - File already present saving AAR fileKey={} iun={} recIndex={}");
    }

    private NotificationInt buildNotificationInt(Instant sentAt) {
        return NotificationInt.builder()
                .iun("IUN")
                .paProtocolNumber("PA123")
                .subject("SUBJECT")
                .paFee(1)
                .sentAt(sentAt)
                .sender(NotificationSenderInt.builder()
                        .paId("pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .build()
                ))
                .build();
    }
}
