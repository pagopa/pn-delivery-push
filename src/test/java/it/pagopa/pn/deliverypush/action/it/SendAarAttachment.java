package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.deliverypush.action.it.mockbean.PaperChannelMock;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelPrepareRequest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.PnSendMode;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX;

public class SendAarAttachment extends CommonTestConfiguration{

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

    @NotNull
    static List<String> replaceSafeStorageKeyFromListAttachment(List<String> attachments) {
        return attachments.stream().map( attachment -> attachment.replace(SAFE_STORAGE_URL_PREFIX, "")).toList();
    }
    
    List<DocumentComposition.TemplateType> getListDocumentTypeGenerated(int times) throws IOException {
        ArgumentCaptor<DocumentComposition.TemplateType> documentTypeCaptor = ArgumentCaptor.forClass(DocumentComposition.TemplateType.class);
        Mockito.verify(documentComposition, Mockito.times(times)).executePdfTemplate(documentTypeCaptor.capture(), Mockito.any());
        return documentTypeCaptor.getAllValues();
    }

    @NotNull
    List<String> getListAttachmentExpectedToSend(PnSendMode currentConf,
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
    static List<String> getDocumentKey(List<NotificationDocumentInt> notificationDocumentList) {
        return notificationDocumentList.stream().map(elem -> elem.getRef().getKey()).toList();
    }

    static String getStringConfiguration(PnSendMode conf) {
        Instant startConfTime = conf.getStartConfigurationTime().truncatedTo(ChronoUnit.SECONDS);
        return String.format("%s;%s;%s;%s;%s",
                startConfTime,
                conf.getAnalogSendAttachmentMode(),
                conf.getSimpleRegisteredLetterSendAttachmentMode(),
                conf.getDigitalSendAttachmentMode(),
                conf.getAarTemplateType());
    }

    static void checkSentAndExpectedAttachmentAreEquals(List<String> listAttachmentExpectedToSend, List<String> prepareAttachmentKeySent) {
        Assertions.assertEquals(listAttachmentExpectedToSend.size(), prepareAttachmentKeySent.size());
        listAttachmentExpectedToSend.forEach(attachmentExpectedToSend -> {
            Assertions.assertTrue(prepareAttachmentKeySent.contains(attachmentExpectedToSend));
        });
    }

    @NotNull
    static List<String> getListAllAttachmentExpectedToSend(String aarKey, List<String> listDocumentKey, List <String> listAttachmentKey) {
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

    List<String> getSentAttachmentKeyFromPrepare() {
        ArgumentCaptor<PaperChannelPrepareRequest> paperChannelPrepareRequestCaptor = ArgumentCaptor.forClass(PaperChannelPrepareRequest.class);
        Mockito.verify(paperChannelMock, Mockito.times(1)).prepare(paperChannelPrepareRequestCaptor.capture());
        PaperChannelPrepareRequest paperChannelPrepareRequest = paperChannelPrepareRequestCaptor.getValue();
        List<String> sentAttachmentKey = paperChannelPrepareRequest.getAttachments();
        //Viene sempre rimossa la stringa safeStorage
        return replaceSafeStorageKeyFromListAttachment(sentAttachmentKey);
    }

    List<String> getSentAttachmentKeyFromSend() {
        ArgumentCaptor<PaperChannelSendRequest> paperChannelSendRequestCaptor = ArgumentCaptor.forClass(PaperChannelSendRequest.class);
        Mockito.verify(paperChannelMock, Mockito.times(1)).send(paperChannelSendRequestCaptor.capture());
        PaperChannelSendRequest paperChannelSendRequest = paperChannelSendRequestCaptor.getValue();
        List<String> sentAttachmentKey = paperChannelSendRequest.getAttachments();
        //Viene sempre rimossa la stringa safeStorage
        return replaceSafeStorageKeyFromListAttachment(sentAttachmentKey);
    }

    String getAarKey(NotificationInt notification, Integer recIndex) {
        String elementId = TimelineEventId.AAR_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        Optional<AarCreationRequestDetailsInt> aarElementDetailsOpt =  timelineService.getTimelineElementDetails(notification.getIun(), elementId, AarCreationRequestDetailsInt.class);
        return aarElementDetailsOpt.map(AarCreationRequestDetailsInt::getAarKey).orElse(null);
    }

}
