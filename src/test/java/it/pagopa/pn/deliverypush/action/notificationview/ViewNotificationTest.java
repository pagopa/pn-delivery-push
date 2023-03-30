package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.Mockito.when;

class ViewNotificationTest {
    @Mock
    private SaveLegalFactsService legalFactStore;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private TimelineService timelineService;
    @Mock
    private AttachmentUtils attachmentUtils;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    @Mock
    private DocumentCreationRequestService documentCreationRequestService;
    @Mock
    private ConfidentialInformationService confidentialInformationService;

    private ViewNotification viewNotification;
    
    private NotificationUtils notificationUtils;
    

    @BeforeEach
    public void setup() {
        when(pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement()).thenReturn(120);
        notificationUtils = new NotificationUtils();
        viewNotification = new ViewNotification(
                legalFactStore,
                documentCreationRequestService,
                timelineUtils, 
                timelineService, 
                attachmentUtils, 
                pnDeliveryPushConfigs,
                confidentialInformationService
        );
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void startVewNotificationProcess() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        String legalFactsId = "legalFactsId";
        when(legalFactStore.sendCreationRequestForNotificationViewedLegalFact(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class), Mockito.isNull(), Mockito.any(Instant.class)))
                .thenReturn(Mono.just(legalFactsId));
        when(attachmentUtils.changeAttachmentsRetention(notification, pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement())).thenReturn(Flux.empty());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        when(timelineUtils.buildNotificationViewedLegalFactCreationRequestTimelineElement(Mockito.eq(notification), Mockito.eq(recIndex),
                        Mockito.eq(legalFactsId), Mockito.isNull(), Mockito.isNull(), Mockito.any()))
                .thenReturn(timelineElementInternal);


                
        Instant viewDate = Instant.now();

        //WHEN
        viewNotification.startVewNotificationProcess(notification, recipient, recIndex, null, null, viewDate).block();

        //THEN

        Mockito.verify(legalFactStore).sendCreationRequestForNotificationViewedLegalFact(notification, recipient, null, viewDate);
                
        Mockito.verify(timelineUtils).buildNotificationViewedLegalFactCreationRequestTimelineElement(Mockito.eq(notification), Mockito.eq(recIndex), 
                Mockito.eq(legalFactsId), Mockito.isNull(), Mockito.isNull(), Mockito.eq(viewDate));

        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);

        Mockito.verify(documentCreationRequestService).addDocumentCreationRequest(legalFactsId, notification.getIun(), recIndex, DocumentCreationTypeInt.RECIPIENT_ACCESS, timelineElementInternal.getElementId());
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void startVewNotificationProcessWithDelegate() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        String legalFactsId = "legalFactsId";
        when(legalFactStore.sendCreationRequestForNotificationViewedLegalFact(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class), Mockito.any(DelegateInfoInt.class), Mockito.any(Instant.class)))
                .thenReturn(Mono.just(legalFactsId));
        
        Instant viewDate = Instant.now();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        when(timelineUtils.buildNotificationViewedLegalFactCreationRequestTimelineElement(Mockito.eq(notification), Mockito.eq(recIndex),
                Mockito.eq(legalFactsId), Mockito.isNull(), Mockito.any(), Mockito.any()))
                .thenReturn(timelineElementInternal);
        
        when(attachmentUtils.changeAttachmentsRetention(notification, pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement())).thenReturn(Flux.empty());

        String internalId = "internalId";
        DelegateInfoInt delegateInfo = DelegateInfoInt.builder()
                .internalId(internalId)
                .delegateType(RecipientTypeInt.PF)
                .mandateId("mandate")
                .build();

        BaseRecipientDtoInt baseRecipientDtoInt = BaseRecipientDtoInt.builder()
                .taxId("testTaxId")
                .denomination("testDenomination")
                .internalId("internalId")
                .build();
        
        when(confidentialInformationService.getRecipientInformationByInternalId(internalId)).thenReturn(Mono.just(baseRecipientDtoInt));
        
        //WHEN
        viewNotification.startVewNotificationProcess(notification, recipient, recIndex, null, delegateInfo, viewDate).block();

        //THEN

        DelegateInfoInt delegateInfoEnriched = delegateInfo.toBuilder()
                .taxId(baseRecipientDtoInt.getTaxId())
                .denomination(baseRecipientDtoInt.getDenomination())
                .build();

        Mockito.verify(timelineUtils).buildNotificationViewedLegalFactCreationRequestTimelineElement(Mockito.eq(notification), Mockito.eq(recIndex),
                Mockito.eq(legalFactsId), Mockito.isNull(), Mockito.eq(delegateInfoEnriched), Mockito.eq(viewDate));

        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);

        Mockito.verify(documentCreationRequestService).addDocumentCreationRequest(legalFactsId, notification.getIun(), recIndex, DocumentCreationTypeInt.RECIPIENT_ACCESS, timelineElementInternal.getElementId());

    }

}
