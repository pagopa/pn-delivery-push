package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.PaperChannelMock;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelPrepareRequest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.PaperSendMode;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX;
import static org.awaitility.Awaitility.await;

class SentAttachmentAndAaarGenerationTestIT extends CommonTestConfiguration{

    @SpyBean
    DocumentComposition documentComposition;
    @SpyBean
    PaperChannelMock paperChannelMock;
    @Autowired
    TimelineService timelineService;
    @Autowired
    StartWorkflowHandler startWorkflowHandler;
    @Autowired
    StatusUtils statusUtils;
    
    @Test
    void AnalogAarDocumentPaymentOldAAR() throws IOException {
        /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Pa physical address presente ed effettua invio con successo
        */
        
        Instant sentNotificationTime = Instant.now();
        
        //Viene valorizzata la configurazione attuale, cioè INSTANT.NOW meno 10 minuti
        PaperSendMode firstCurrentConf = PaperSendMode.builder()
                .startConfigurationTime(sentNotificationTime.minus(10, ChronoUnit.MINUTES))
                .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION)
                .build();
        final String firstCurrentConfString = getStringConfiguration(firstCurrentConf);

        //Viene valorizzata la configurazione futura, cioè INSTANT.NOW più 10 giorni
        PaperSendMode secondConf = PaperSendMode.builder()
                .startConfigurationTime(sentNotificationTime.plus(10, ChronoUnit.DAYS))
                .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION)
                .build();
        final String secondConfString = getStringConfiguration(secondConf);

        List<String> paperSendModeList = new ArrayList<>();
        paperSendModeList.add(firstCurrentConfString);
        paperSendModeList.add(secondConfString);
        
        Mockito.when(cfg.getPaperSendMode()).thenReturn(paperSendModeList);

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + " Via Nuova")
                .build();

        String taxId01 = "TAXID01";

        String pagoPaAttachment = "thisIsAnAttachment";
        List<NotificationDocumentInt> pagoPaAttachmentList = TestUtils.getDocumentList(pagoPaAttachment);

        PagoPaInt paGoPaPayment= PagoPaInt.builder()
                .creditorTaxId("cred")
                .noticeCode("notice")
                .attachment(pagoPaAttachmentList.get(0))
                .build();
        
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withPhysicalAddress(paPhysicalAddress)
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(paGoPaPayment)
                                .build()
                ))
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        List<TestUtils.DocumentWithContent> listAttachmentWithContent = TestUtils.getDocumentWithContents(pagoPaAttachment, pagoPaAttachmentList);
        TestUtils.firstFileUploadFromNotification(listAttachmentWithContent, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withSentAt(sentNotificationTime)
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

        final List<String> listAttachmentExpectedToSend = getListAttachmentExpectedToSend(firstCurrentConf, notification, recIndex, notificationDocumentList, pagoPaAttachmentList);

        //Vengono ottenuti gli attachment inviati nella richiesta di PREPARE verso paperChannel
        final List<String> prepareAttachmentKeySent = getSentAttachmentKeyFromPrepare();
        //Viene verificata che gli attachment inviati in fase di PREPARE siano esattamente quelli attesi
        checkSentAndExpectedAttachmentAreEquals(listAttachmentExpectedToSend, prepareAttachmentKeySent);

        //Vengono ottenuti gli attachment inviati nella richiesta di SEND verso paperChannel
        final List<String> sendAttachmentKeySent = getSentAttachmentKeyFromPrepare();
        //Viene verificata che gli attachment inviati in fase di SEND siano esattamente quelli attesi
        checkSentAndExpectedAttachmentAreEquals(listAttachmentExpectedToSend, sendAttachmentKeySent);
        
        //Viene ottenuta la lista di tutti i documenti generati
        final List<DocumentComposition.TemplateType> listDocumentTypeGenerated = getListDocumentTypeGenerated(2);
        //Viene quindi verificato se nella lista dei documenti generati c'è il documento atteso
        Assertions.assertTrue(listDocumentTypeGenerated.contains(firstCurrentConf.getAarTemplateType()));
    }

    @Test
    void SimpleRegisteredLetterAarDocumentOldAAR() throws IOException {
        /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Pa physical address presente ed effettua invio con successo
        */
        
        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        
        Instant sentNotificationTime = Instant.now();

        //Viene valorizzata la configurazione attuale, cioè INSTANT.NOW meno 10 minuti
        PaperSendMode firstCurrentConf = PaperSendMode.builder()
                .startConfigurationTime(sentNotificationTime.minus(10, ChronoUnit.MINUTES))
                .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS)
                .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION)
                .build();
        final String firstCurrentConfString = getStringConfiguration(firstCurrentConf);

        //Viene valorizzata la configurazione futura, cioè INSTANT.NOW più 10 giorni
        PaperSendMode secondConf = PaperSendMode.builder()
                .startConfigurationTime(sentNotificationTime.plus(10, ChronoUnit.DAYS))
                .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION)
                .build();
        final String secondConfString = getStringConfiguration(secondConf);

        List<String> paperSendModeList = new ArrayList<>();
        paperSendModeList.add(firstCurrentConfString);
        paperSendModeList.add(secondConfString);

        Mockito.when(cfg.getPaperSendMode()).thenReturn(paperSendModeList);

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + " Via Nuova")
                .build();

        String taxId01 = "TAXID01";

        String pagoPaAttachment = "thisIsAnAttachment";
        List<NotificationDocumentInt> pagoPaAttachmentList = TestUtils.getDocumentList(pagoPaAttachment);

        PagoPaInt paGoPaPayment= PagoPaInt.builder()
                .creditorTaxId("cred")
                .noticeCode("notice")
                .attachment(pagoPaAttachmentList.get(0))
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withPhysicalAddress(paPhysicalAddress)
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(paGoPaPayment)
                                .build()
                ))
                .withDigitalDomicile(digitalDomicile)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        List<TestUtils.DocumentWithContent> listAttachmentWithContent = TestUtils.getDocumentWithContents(pagoPaAttachment, pagoPaAttachmentList);
        TestUtils.firstFileUploadFromNotification(listAttachmentWithContent, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withSentAt(sentNotificationTime)
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

        final List<String> listAttachmentExpectedToSend = getListAttachmentExpectedToSend(firstCurrentConf, notification, recIndex, notificationDocumentList, pagoPaAttachmentList);

        //Vengono ottenuti gli attachment inviati nella richiesta di PREPARE verso paperChannel
        final List<String> prepareAttachmentKeySent = getSentAttachmentKeyFromPrepare();
        //Viene verificata che gli attachment inviati in fase di PREPARE siano esattamente quelli attesi
        checkSentAndExpectedAttachmentAreEquals(listAttachmentExpectedToSend, prepareAttachmentKeySent);

        //Vengono ottenuti gli attachment inviati nella richiesta di SEND verso paperChannel
        final List<String> sendAttachmentKeySent = getSentAttachmentKeyFromPrepare();
        //Viene verificata che gli attachment inviati in fase di SEND siano esattamente quelli attesi
        checkSentAndExpectedAttachmentAreEquals(listAttachmentExpectedToSend, sendAttachmentKeySent);

        //Viene ottenuta la lista di tutti i documenti generati
        final List<DocumentComposition.TemplateType> listDocumentTypeGenerated = getListDocumentTypeGenerated(3);
        //Viene quindi verificato se nella lista dei documenti generati c'è il documento atteso
        Assertions.assertTrue(listDocumentTypeGenerated.contains(firstCurrentConf.getAarTemplateType()));
    }
    
    @Test
    void analogAarNewAAR() throws IOException {
        /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Pa physical address presente ed effettua invio con successo
        */

        Instant sentNotificationTime = Instant.now();

        //Viene valorizzata la configurazione vecchia, cioè INSTANT.NOW meno 10 giorni
        PaperSendMode notCurrentConf = PaperSendMode.builder()
                .startConfigurationTime(sentNotificationTime.minus(10, ChronoUnit.DAYS))
                .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION)
                .build();
        final String notCurrentConfString = getStringConfiguration(notCurrentConf);

        //Viene valorizzata la configurazione attuale, cioè INSTANT.NOW meno 1 giorni
        PaperSendMode currentConf = PaperSendMode.builder()
                .startConfigurationTime(sentNotificationTime.minus(1, ChronoUnit.DAYS))
                .analogSendAttachmentMode(SendAttachmentMode.AAR)
                .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION_RADD)
                .build();
        final String currentConfString = getStringConfiguration(currentConf);

        List<String> paperSendModeList = new ArrayList<>();
        paperSendModeList.add(notCurrentConfString);
        paperSendModeList.add(currentConfString);

        Mockito.when(cfg.getPaperSendMode()).thenReturn(paperSendModeList);

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + " Via Nuova")
                .build();

        String taxId01 = "TAXID01";

        String pagoPaAttachment = "thisIsAnAttachment";
        List<NotificationDocumentInt> pagoPaAttachmentList = TestUtils.getDocumentList(pagoPaAttachment);

        PagoPaInt paGoPaPayment= PagoPaInt.builder()
                .creditorTaxId("cred")
                .noticeCode("notice")
                .attachment(pagoPaAttachmentList.get(0))
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withPhysicalAddress(paPhysicalAddress)
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(paGoPaPayment)
                                .build()
                ))
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        List<TestUtils.DocumentWithContent> listAttachmentWithContent = TestUtils.getDocumentWithContents(pagoPaAttachment, pagoPaAttachmentList);
        TestUtils.firstFileUploadFromNotification(listAttachmentWithContent, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withSentAt(sentNotificationTime)
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

        final List<String> listAttachmentExpectedToSend = getListAttachmentExpectedToSend(currentConf, notification, recIndex, notificationDocumentList, pagoPaAttachmentList);

        //Vengono ottenuti gli attachment inviati nella richiesta di PREPARE verso paperChannel
        final List<String> prepareAttachmentKeySent = getSentAttachmentKeyFromPrepare();
        //Viene verificata che gli attachment inviati in fase di PREPARE siano esattamente quelli attesi
        checkSentAndExpectedAttachmentAreEquals(listAttachmentExpectedToSend, prepareAttachmentKeySent);

        //Vengono ottenuti gli attachment inviati nella richiesta di SEND verso paperChannel
        final List<String> sendAttachmentKeySent = getSentAttachmentKeyFromPrepare();
        //Viene verificata che gli attachment inviati in fase di SEND siano esattamente quelli attesi
        checkSentAndExpectedAttachmentAreEquals(listAttachmentExpectedToSend, sendAttachmentKeySent);

        //Viene ottenuta la lista di tutti i documenti generati
        final List<DocumentComposition.TemplateType> listDocumentTypeGenerated = getListDocumentTypeGenerated(2);
        //Viene quindi verificato se nella lista dei documenti generati c'è il documento atteso
        Assertions.assertTrue(listDocumentTypeGenerated.contains(currentConf.getAarTemplateType()));
    }

    private List<DocumentComposition.TemplateType> getListDocumentTypeGenerated(int times) throws IOException {
        ArgumentCaptor<DocumentComposition.TemplateType> documentTypeCaptor = ArgumentCaptor.forClass(DocumentComposition.TemplateType.class);
        Mockito.verify(documentComposition, Mockito.times(times)).executePdfTemplate(documentTypeCaptor.capture(), Mockito.any());
        return documentTypeCaptor.getAllValues();
    }

    @NotNull
    private List<String> getListAttachmentExpectedToSend(PaperSendMode currentConf, 
                                                         NotificationInt notification, 
                                                         Integer recIndex, 
                                                         List<NotificationDocumentInt> notificationDocumentList, 
                                                         List<NotificationDocumentInt> pagoPaAttachmentList) {
        List<String> listAttachmentExpectedToSend = new ArrayList<>();

        if(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS.equals(currentConf.getAnalogSendAttachmentMode()) ||
                SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS.equals(currentConf.getSimpleRegisteredLetterSendAttachmentMode())){
            //Viene create una lista di tutti i documenti che ci si aspetta siano stati spediti -> AAR + documenti notifica + attachment di pagamento
            String aarKey = getAarKey(notification, recIndex);
            List <String> listDocumentKey = getDocumentKey(notificationDocumentList);
            List <String> listAttachmentKey = getDocumentKey(pagoPaAttachmentList);
            listAttachmentExpectedToSend = getListAllAttachmentExpectedToSend( aarKey, listDocumentKey,listAttachmentKey);
        }
        else if(SendAttachmentMode.AAR_DOCUMENTS.equals(currentConf.getAnalogSendAttachmentMode()) ||
                SendAttachmentMode.AAR_DOCUMENTS.equals(currentConf.getSimpleRegisteredLetterSendAttachmentMode())){
            //Viene create una lista di tutti i documenti che ci si aspetta siano stati spediti -> AAR + documenti notifica
            String aarKey = getAarKey(notification, recIndex);
            List <String> listDocumentKey = getDocumentKey(notificationDocumentList);
            listAttachmentExpectedToSend = getListAllAttachmentExpectedToSend( aarKey, listDocumentKey,null);
        }else if(SendAttachmentMode.AAR.equals(currentConf.getAnalogSendAttachmentMode()) ||
                SendAttachmentMode.AAR.equals(currentConf.getSimpleRegisteredLetterSendAttachmentMode())){
            //Viene create una lista di tutti i documenti che ci si aspetta siano stati spediti -> AAR
            String aarKey = getAarKey(notification, recIndex);
            listAttachmentExpectedToSend = getListAllAttachmentExpectedToSend( aarKey, null, null);
        }
        return listAttachmentExpectedToSend;
    }

    @NotNull
    private static List<String> getDocumentKey(List<NotificationDocumentInt> notificationDocumentList) {
        return notificationDocumentList.stream().map(elem -> elem.getRef().getKey()).toList();
    }

    private static String getStringConfiguration(PaperSendMode firstCurrentConf) {
        Instant startConfTime = firstCurrentConf.getStartConfigurationTime().truncatedTo(ChronoUnit.SECONDS);
        return String.format("%s;%s;%s;%s",
                startConfTime,
                firstCurrentConf.getAnalogSendAttachmentMode(),
                firstCurrentConf.getSimpleRegisteredLetterSendAttachmentMode(),
                firstCurrentConf.getAarTemplateType());
    }

    private static void checkSentAndExpectedAttachmentAreEquals(List<String> listAttachmentExpectedToSend, List<String> prepareAttachmentKeySent) {
        Assertions.assertEquals(listAttachmentExpectedToSend.size(), prepareAttachmentKeySent.size());
        listAttachmentExpectedToSend.forEach(attachmentExpectedToSend -> {
            Assertions.assertTrue(prepareAttachmentKeySent.contains(attachmentExpectedToSend));
        });
    }

    @NotNull
    private static List<String> getListAllAttachmentExpectedToSend(String aarKey, List<String> listDocumentKey, List <String> listAttachmentKey) {
        List<String> listAttachmentExpectedToSend = new ArrayList<>();
        if(listDocumentKey != null){
            listAttachmentExpectedToSend.addAll(listDocumentKey);
        }
        if(aarKey != null){
            listAttachmentExpectedToSend.add(aarKey);
        }
        if(listAttachmentKey !=  null){
            listAttachmentExpectedToSend.addAll(listAttachmentKey);
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
