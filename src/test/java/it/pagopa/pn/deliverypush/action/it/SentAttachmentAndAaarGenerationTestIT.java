package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.PaperChannelMock;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelPrepareRequest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX;
import static org.awaitility.Awaitility.await;

class SentAttachmentAndAaarGenerationTestIT extends CommonTestConfiguration{
    @SpyBean
    PaperChannelMock paperChannelMock;
    @Autowired
    TimelineService timelineService;
    @Autowired
    StartWorkflowHandler startWorkflowHandler;
    @Autowired
    StatusUtils statusUtils;
    
    @Test
    void checkConfigurationWithAttachmentAndOldAAR() {
 /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Pa physical address presente ed effettua invio con successo
     */

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + " Via Nuova")
                .build();

        String taxId01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withPhysicalAddress(paPhysicalAddress)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();
        
        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );
        
        //Viene create una lista di tutti gli attachment che ci si aspetta siano stati spediti -> documenti notifica + aar
        String aarKey = getAarKey(notification, recIndex);
        List <String> listDocumentKey = notificationDocumentList.stream().map(elem -> elem.getRef().getKey()).toList();
        final List<String> listAttachmentExpectedToSend = getListAllAttachmentExpectedToSend(listDocumentKey, aarKey);

        //Vengono ottenuti gli attachment inviati nella richiesta di PREPARE verso paperChannel
        final List<String> prepareAttachmentKeySent = getSentAttachmentKeyFromPrepare();
        //Viene verificata che gli attachment inviati in fase di PREPARE siano esattamente quelli attesi
        checkSentAndExpectedAttachmentAreEquals(listAttachmentExpectedToSend, prepareAttachmentKeySent);

        //Vengono ottenuti gli attachment inviati nella richiesta di SEND verso paperChannel
        final List<String> sendAttachmentKeySent = getSentAttachmentKeyFromPrepare();
        //Viene verificata che gli attachment inviati in fase di SEND siano esattamente quelli attesi
        checkSentAndExpectedAttachmentAreEquals(listAttachmentExpectedToSend, sendAttachmentKeySent);
        

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    private static void checkSentAndExpectedAttachmentAreEquals(List<String> listAttachmentExpectedToSend, List<String> prepareAttachmentKeySent) {
        Assertions.assertEquals(listAttachmentExpectedToSend.size(), prepareAttachmentKeySent.size());
        listAttachmentExpectedToSend.forEach(attachmentExpectedToSend -> {
            Assertions.assertTrue(prepareAttachmentKeySent.contains(attachmentExpectedToSend));
        });
    }

    @NotNull
    private static List<String> getListAllAttachmentExpectedToSend(List<String> listDocumentKey, String aarKey) {
        List<String> listAttachmentExpectedToSend = new ArrayList<>();
        if(listDocumentKey != null){
            listAttachmentExpectedToSend.addAll(listDocumentKey);
        }
        if(aarKey != null){
            listAttachmentExpectedToSend.add(aarKey);
        }

        return replaceSafeStorageKeyFromListAttachment(listAttachmentExpectedToSend);
    }

    private List<String> getSentAttachmentKeyFromPrepare() {
        ArgumentCaptor<PaperChannelPrepareRequest> paperChannelPrepareRequestCaptor = ArgumentCaptor.forClass(PaperChannelPrepareRequest.class);
        Mockito.verify(paperChannelMock, Mockito.times(1)).prepare(paperChannelPrepareRequestCaptor.capture());
        PaperChannelPrepareRequest paperChannelPrepareRequest = paperChannelPrepareRequestCaptor.getValue();
        List<String> sentAttachmentKey = paperChannelPrepareRequest.getAttachments();
        //Viene sempre rimossa la stringa safeStorage
        return replaceSafeStorageKeyFromListAttachment(sentAttachmentKey);
    }
    
    private List<String> getSentAttachmentKeyFromSend() {
        ArgumentCaptor<PaperChannelSendRequest> paperChannelSendRequestCaptor = ArgumentCaptor.forClass(PaperChannelSendRequest.class);
        Mockito.verify(paperChannelMock, Mockito.times(1)).send(paperChannelSendRequestCaptor.capture());
        PaperChannelSendRequest paperChannelSendRequest = paperChannelSendRequestCaptor.getValue();
        List<String> sentAttachmentKey = paperChannelSendRequest.getAttachments();
        //Viene sempre rimossa la stringa safeStorage
        return replaceSafeStorageKeyFromListAttachment(sentAttachmentKey);
    }
    
    private String getAarKey(NotificationInt notification, Integer recIndex) {
        String elementId = TimelineEventId.AAR_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        Optional<AarCreationRequestDetailsInt> aarElementDetailsOpt =  timelineService.getTimelineElementDetails(notification.getIun(), elementId, AarCreationRequestDetailsInt.class);
        return aarElementDetailsOpt.map(AarCreationRequestDetailsInt::getAarKey).orElse(null);
    }

    @NotNull
    private static List<String> replaceSafeStorageKeyFromListAttachment(List<String> attachments) {
        return attachments.stream().map( attachment -> attachment.replace(SAFE_STORAGE_URL_PREFIX, "")).toList();
    }

}
