package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.*;
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
    private InstantNowSupplier instantNowSupplier;
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
    
    private ViewNotification viewNotification;
    
    private NotificationUtils notificationUtils;
    

    @BeforeEach
    public void setup() {
        when(pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement()).thenReturn(120);
        notificationUtils = new NotificationUtils();
        viewNotification = new ViewNotification(
                instantNowSupplier, 
                legalFactStore,
                documentCreationRequestService,
                timelineUtils, 
                timelineService, 
                attachmentUtils, 
                pnDeliveryPushConfigs
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
        when(legalFactStore.sendCreationRequestForNotificationViewedLegalFact(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class), Mockito.any(Instant.class)))
                .thenReturn(Mono.just(legalFactsId));
        when(instantNowSupplier.get()).thenReturn(Instant.now());
        when(attachmentUtils.changeAttachmentsRetention(notification, pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement())).thenReturn(Flux.empty());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        when(timelineUtils.buildNotificationViewedLegalFactCreationRequestTimelineElement(Mockito.eq(notification), Mockito.eq(recIndex),
                        Mockito.eq(legalFactsId), Mockito.isNull(), Mockito.isNull(), Mockito.any()))
                .thenReturn(timelineElementInternal);
        
        Instant viewDate = Instant.now();

        //WHEN
        viewNotification.startVewNotificationProcess(notification, recipient, recIndex, null, null, viewDate).block();

        //THEN
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
        when(legalFactStore.sendCreationRequestForNotificationViewedLegalFact(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class), Mockito.any(Instant.class)))
                .thenReturn(Mono.just(legalFactsId));
        
        when(instantNowSupplier.get()).thenReturn(Instant.now());
        Instant viewDate = Instant.now();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        when(timelineUtils.buildNotificationViewedLegalFactCreationRequestTimelineElement(Mockito.eq(notification), Mockito.eq(recIndex),
                Mockito.eq(legalFactsId), Mockito.isNull(), Mockito.any(), Mockito.any()))
                .thenReturn(timelineElementInternal);
        
        when(attachmentUtils.changeAttachmentsRetention(notification, pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement())).thenReturn(Flux.empty());
        
        //WHEN
        String internalId = "internalId";

        DelegateInfoInt delegateInfo = DelegateInfoInt.builder()
                .internalId(internalId)
                .delegateType(RecipientTypeInt.PF)
                .mandateId("mandate")
                .build();
        
        viewNotification.startVewNotificationProcess(notification, recipient, recIndex, null, delegateInfo, viewDate).block();

        //THEN
        
        Mockito.verify(timelineUtils).buildNotificationViewedLegalFactCreationRequestTimelineElement(Mockito.eq(notification), Mockito.eq(recIndex),
                Mockito.eq(legalFactsId), Mockito.isNull(), Mockito.eq(delegateInfo), Mockito.eq(viewDate));

        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);

        Mockito.verify(documentCreationRequestService).addDocumentCreationRequest(legalFactsId, notification.getIun(), recIndex, DocumentCreationTypeInt.RECIPIENT_ACCESS, timelineElementInternal.getElementId());

    }

}
