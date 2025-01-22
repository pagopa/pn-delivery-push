package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.deliverypush.action.it.mockbean.PaperChannelMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.TimelineDaoMock;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AnalogWorfklowRecipientDeceasedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.awaitility.Awaitility.await;

@Disabled("Test fail sometimes")
class NotificationCostDeceasedTestIT extends CommonTestConfiguration {
    @Autowired
    StartWorkflowHandler startWorkflowHandler;
    @SpyBean
    TimelineService timelineService;
    @Autowired
    StatusUtils statusUtils;
    @Autowired
    NotificationViewedRequestHandler notificationViewedRequestHandler;
    @SpyBean
    private AttachmentUtils attachmentUtils;

    @Test
    void notificationViewedAfterRecipientDeceasedWorkflow() {
        // Scenario: Notifica riceve evento DECEDUTO (ANALOG_WORKFLOW_RECIPIENT_DECEASED)
        // Se in seguito arriva l'evento di visualizzazione (NOTIFICATION_VIEW)
        // accertarsi che non sia aggiornata 2 volte la retention e non sia aggiunto il costo nella timeline di visualizzazione (NOTIFICATION_VIEW).

        Mockito.when(cfg.getActivationDeceasedWorkflowDate()).thenReturn("2021-10-10T00:00:00Z");

        // GIVEN
        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_SEND_DECEASED)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest")
                .withTaxId(TestUtils.getTaxId())
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);
        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.emptyList());

        // Ottenimento dell'IUN (Identificativo Unico Notifica) e dell'indice del destinatario
        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        // Start del workflow della notifica
        startWorkflowHandler.startWorkflow(iun);

        // Verifica che l'evento di decesso sia stato registrato correttamente
        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentAnalogWorkflowRecipientDeceased(iun, recIndex, timelineService)
                )
        );

        // Attesa fino a che lo stato della notifica non diventi RETURNED_TO_SENDER
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.RETURNED_TO_SENDER, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        // Simulazione della visualizzazione della notifica
        Instant notificationViewDate = Instant.now();
        notificationViewedRequestHandler.handleViewNotificationDelivery(iun, recIndex, null, notificationViewDate);

        // Attesa fino a che in timeline ci sia l'evento di visualizzazione
        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentViewed(iun, recIndex, timelineService)
                )
        );

        // Verifica che lo stato della notifica resti RETURNED_TO_SENDER
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.RETURNED_TO_SENDER, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        // Verifica che il costo non sia stato aggiunto nella timeline di visualizzazione
        checkCostForDeceasedEvent(iun, recIndex, true);
        checkCostForViewEvent(iun, recIndex, false);
    }

    @Test
    void notificationViewedBeforeRecipientDeceasedWorkflow() {
        // Scenario: Notifica viene creata e VISUALIZZATA prima di ricevere un evento di DECEDUTO (ANALOG_WORKFLOW_RECIPIENT_DECEASED)
        // Accertarsi che l'evento di deceduto non comporti un aggiornamento della retention dei documenti
        // e nel dettaglio dell'elemento di timeline (ANALOG_WORKFLOW_RECIPIENT_DECEASED) non venga aggiunto il costo.
        Mockito.when(cfg.getActivationDeceasedWorkflowDate()).thenReturn("2021-10-10T00:00:00Z");

        // GIVEN
        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_SEND_DECEASED)
                .build();
        String iun = TestUtils.getRandomIun();

        String taxId = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +
                TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(0)
                                .sentAttemptMade(0)
                                .build()
                );


        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest")
                .withTaxId(taxId)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);
        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.emptyList());

        // Ottenimento dell'IUN (Identificativo Unico Notifica) e dell'indice del destinatario
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        // Start del workflow della notifica
        startWorkflowHandler.startWorkflow(iun);

        // Verifica che l'evento di decesso sia stato registrato correttamente
        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentAnalogWorkflowRecipientDeceased(iun, recIndex, timelineService)
                )
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentViewed(iun, recIndex, timelineService)
                )
        );

        // Verifica che il costo sia stato aggiunto nella timeline di visualizzazione e non in quella di decesso
        checkCostForViewEvent(iun, recIndex, true);
        checkCostForDeceasedEvent(iun, recIndex, false);
    }

    private void checkCostForViewEvent(String iun, Integer recIndex, boolean isCostExpected) {
        // Verifica che l'evento abbia o meno il costo in base al parametro isCostExpected
        Optional<TimelineElementInternal> elementOpt = timelineService.getTimelineElement(iun, TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        ));
        Assertions.assertTrue(elementOpt.isPresent());
        TimelineElementInternal element = elementOpt.get();
        NotificationViewedDetailsInt details = (NotificationViewedDetailsInt) element.getDetails();

        if (isCostExpected) {
            Assertions.assertNotNull(details.getNotificationCost());
        } else {
            Assertions.assertNull(details.getNotificationCost());
        }
    }

    private void checkCostForDeceasedEvent(String iun, Integer recIndex, boolean isCostExpected) {
        // Verifica che l'evento abbia o meno il costo in base al parametro isCostExpected
        Optional<TimelineElementInternal> elementOpt = timelineService.getTimelineElement(iun, TimelineEventId.ANALOG_WORKFLOW_RECIPIENT_DECEASED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        ));
        Assertions.assertTrue(elementOpt.isPresent());
        TimelineElementInternal element = elementOpt.get();
        AnalogWorfklowRecipientDeceasedDetailsInt details = (AnalogWorfklowRecipientDeceasedDetailsInt) element.getDetails();

        if (isCostExpected) {
            Assertions.assertNotNull(details.getNotificationCost());
        } else {
            Assertions.assertNull(details.getNotificationCost());
        }
    }
}