package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.deliverypush.action.checkattachmentretention.CheckAttachmentRetentionHandler;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static org.awaitility.Awaitility.await;

class CheckAttachmentRetentionIT extends CommonTestConfiguration {
    @SpyBean
    SchedulerService schedulerService;
    @SpyBean
    CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;
    @Autowired
    StartWorkflowHandler startWorkflowHandler;
    @Autowired
    TimelineService timelineService;
    @SpyBean
    AttachmentUtils attachmentUtils;
    @Autowired
    NotificationViewedRequestHandler notificationViewedRequestHandler;
    
    @Test
    void verifyCheckAttachmentAlreadyRefined() {
        String iun = TestUtils.getRandomIun();

        String taxId = "taxIdTest";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withIun(iun)
                .withNotificationRecipient(recipient)
                .build();
        
        pnDeliveryClientMock.addNotification(notification);

        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        final Duration attachmentRetentionTimeAfterValidation = Duration.ofSeconds(20);
        final Duration checkAttachmentTimeBeforeExpiration = Duration.ofSeconds(1);
        TimeParams times = cfg.getTimeParams();
        times.setAttachmentRetentionTimeAfterValidation(attachmentRetentionTimeAfterValidation);
        times.setCheckAttachmentTimeBeforeExpiration(checkAttachmentTimeBeforeExpiration);
        Mockito.when(cfg.getTimeParams()).thenReturn(times);
    //    Mockito.when(cfg.getTimeParams().getCheckAttachmentTimeBeforeExpiration()).thenReturn(Duration.ofSeconds(10));
    //    Mockito.when(cfg.getTimeParams().getAttachmentTimeToAddAfterExpiration()).thenReturn(Duration.ofSeconds(20));


        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che non sia stato inserito l'elemento SENDERACK_CREATION_REQUEST
        String timelineId = TimelineEventId.REQUEST_ACCEPTED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build()
        );
        await().untilAsserted(() -> Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent()));

        //Simulazione visualizzazione della notifica, che comporta il perfezionamento
        notificationViewedRequestHandler.handleViewNotificationDelivery(iun, recIndex, null, Instant.now());
        
        //Si attende fino a che non scada il tempo di scheduling del check attachment
        TimelineElementInternal senderAckCreationRequest = timelineService.getTimelineElement(iun, timelineId).get();
        Instant dateToWait = senderAckCreationRequest.getTimestamp().plus(attachmentRetentionTimeAfterValidation.plus(Duration.ofSeconds(5)));
        await()
                .atMost(200, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertTrue(Instant.now().isAfter(dateToWait)));
        
        //Viene quindi verificato che il metodo di check attachment sia stato effettivamente richiamato ...
        Mockito.verify(checkAttachmentRetentionHandler).handleCheckAttachmentRetentionBeforeExpiration(iun);
        //... ma che non abbia effettuato l'update della retention perchè la notifica è già perfezionata (per presa visione)
        Mockito.verify(attachmentUtils, Mockito.times(1)).changeAttachmentsRetention(Mockito.eq(notification), Mockito.anyInt());
        // ... e che lo scheduling del check sia dunque avvenuto una sola volta
        Mockito.verify(schedulerService, Mockito.times(1)).scheduleEvent(Mockito.eq(iun), Mockito.any(Instant.class), Mockito.eq(ActionType.CHECK_ATTACHMENT_RETENTION));

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void verifyCheckAttachmentNotRefined() {
        String iun = TestUtils.getRandomIun();

        String taxId = "taxIdTest";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withIun(iun)
                .withNotificationRecipient(recipient)
                .build();

        pnDeliveryClientMock.addNotification(notification);

        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        final Duration attachmentRetentionTimeAfterValidation = Duration.ofSeconds(2);
        final Duration checkAttachmentTimeBeforeExpiration = Duration.ofSeconds(1);
        final Duration attachmentTimeToAddAfterExpiration = Duration.ofSeconds(20);
        TimeParams times = cfg.getTimeParams();
        times.setAttachmentRetentionTimeAfterValidation(attachmentRetentionTimeAfterValidation);
        times.setCheckAttachmentTimeBeforeExpiration(checkAttachmentTimeBeforeExpiration);
        times.setAttachmentTimeToAddAfterExpiration(attachmentTimeToAddAfterExpiration);
        Mockito.when(cfg.getTimeParams()).thenReturn(times);
        //    Mockito.when(cfg.getTimeParams().getAttachmentTimeToAddAfterExpiration()).thenReturn(Duration.ofSeconds(20));
        
        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che non sia stato inserito l'elemento REFINEMENT
        await().untilAsserted(() -> {
            String timelineId = TimelineEventId.REFINEMENT.buildEventId(
                            EventId.builder()
                                    .iun(iun)
                                    .recIndex(recIndex)
                                    .build()
                    );
            Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent());
        });
        
        //Si attende fino a che non scada il tempo di scheduling del check attachment
        String timelineId = TimelineEventId.SENDERACK_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build()
        );
        TimelineElementInternal senderAckCreationRequest = timelineService.getTimelineElement(iun, timelineId).get();
        Instant dateToWait = senderAckCreationRequest.getTimestamp().plus(attachmentRetentionTimeAfterValidation.plus(Duration.ofSeconds(5)));
        await()
                .atMost(200, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertTrue(Instant.now().isAfter(dateToWait)));

        //Viene quindi verificato che il metodo di check attacment sia stato effettivamente richiamato ...
        Mockito.verify(checkAttachmentRetentionHandler).handleCheckAttachmentRetentionBeforeExpiration(iun);
        //... ma che non abbia effettuato l'update della retention perchè la notifica è già perfezionata (per presa visione)
        Mockito.verify(attachmentUtils, Mockito.times(2)).changeAttachmentsRetention(Mockito.eq(notification), Mockito.anyInt());
        //... e che lo scheduling del check retention sia avvenuto una seconda volta
        Mockito.verify(schedulerService, Mockito.times(2)).scheduleEvent(Mockito.eq(iun), Mockito.any(Instant.class), Mockito.eq(ActionType.CHECK_ATTACHMENT_RETENTION));
        
        ConsoleAppenderCustom.checkLogs();
    }

}
