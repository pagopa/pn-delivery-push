package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.refinement.RefinementHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.Mockito.when;

class RefinementHandlerTest {

    @Mock
    private TimelineService timelineService;
    
    @Mock
    private TimelineUtils timelineUtils;
    
    @Mock
    private NotificationService notificationService;
    
    @Mock
    private NotificationProcessCostService notificationProcessCostService;

    @Mock
    private AttachmentUtils attachmentUtils;

    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    
    private RefinementHandler refinementHandler;

    @BeforeEach
    public void setup() {
        when(pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement()).thenReturn(120);
        refinementHandler = new RefinementHandler(timelineService,
                timelineUtils, notificationService, notificationProcessCostService, attachmentUtils, pnDeliveryPushConfigs);
    }
    @ExtendWith(MockitoExtension.class)
    @Test
    void handleRefinement() {
        String iun = "I01";
        Integer recIndex = 1;
        NotificationInt notification = getNotificationWithPhysicalAddress();
        
        when(timelineUtils.checkNotificationIsAlreadyViewed(iun, recIndex)).thenReturn(Boolean.FALSE);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(notificationProcessCostService.getNotificationProfit(notification, recIndex)).thenReturn(100);

        refinementHandler.handleRefinement(iun, recIndex);
        
        Mockito.verify(timelineUtils, Mockito.times(1)).buildRefinementTimelineElement(notification,
                recIndex, 100);
        
    }

    
    private NotificationInt getNotificationWithPhysicalAddress() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .physicalAddress(
                                        PhysicalAddressInt.builder()
                                                .address("test address")
                                                .build()
                                )
                                .payment(null)
                                .build()
                ))
                .build();
    }
}