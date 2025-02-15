package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notificationviewed.NotificationViewedInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.RecipientType;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.RequestNotificationViewedDto;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class NotificationViewedRequestHandlerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private TimelineService timelineService;
    @Mock
    private StatusUtils statusUtils;
    @Mock
    private ViewNotification viewNotification;
    @Mock
    private PaperNotificationFailedService paperNotificationFailedService;

    private NotificationViewedRequestHandler handler;

    @BeforeEach
    public void setup() {
        NotificationUtils notificationUtils = new NotificationUtils();

        handler = new NotificationViewedRequestHandler(timelineService, notificationService,
                timelineUtils, statusUtils, notificationUtils, viewNotification, paperNotificationFailedService);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void handleViewNotificationDelivery() {
        //GIVEN
        String iun = "test_iun";
        NotificationInt notification = getNotification(iun);
        NotificationRecipientInt recipientInt = notification.getRecipients().get(0);

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(viewNotification.startVewNotificationProcess(Mockito.any(NotificationInt.class), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        
        Instant viewDate = Instant.now();
        int recIndex = 0;

        //WHEN
        NotificationViewedInt notificationViewedInt = buildNotificationViewedInt(iun, recIndex, viewDate, null, null);
        handler.handleViewNotificationDelivery(notificationViewedInt);
        
        //THEN
        
        Mockito.verify(viewNotification).startVewNotificationProcess(
                Mockito.eq(notification),
                Mockito.eq(recipientInt),
                Mockito.eq(notificationViewedInt)
        );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleViewNotificationDeliveryWithDelegate() {
        //GIVEN
        String iun = "test_iun";
        NotificationInt notification = getNotification(iun);
        NotificationRecipientInt recipientInt = notification.getRecipients().get(0);

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(viewNotification.startVewNotificationProcess(Mockito.any(NotificationInt.class), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Instant viewDate = Instant.now();
        int recIndex = 0;

        //WHEN
        DelegateInfoInt delegateInfo = DelegateInfoInt.builder().build();
        NotificationViewedInt notificationViewedInt = buildNotificationViewedInt(iun, recIndex, viewDate, delegateInfo, null);
        handler.handleViewNotificationDelivery(notificationViewedInt);

        //THEN

        Mockito.verify(viewNotification).startVewNotificationProcess(
                Mockito.eq(notification),
                Mockito.eq(recipientInt),
                Mockito.eq(notificationViewedInt)
        );
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void handleViewNotificationRadd() {
        //GIVEN
        String iun = "test_iun";
        NotificationInt notification = getNotification(iun);
        NotificationRecipientInt recipientInt = notification.getRecipients().get(0);

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(viewNotification.startVewNotificationProcess(Mockito.any(NotificationInt.class), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Instant viewDate = Instant.now();
        int recIndex = 0;

        RaddInfo raddInfo = RaddInfo.builder()
                .transactionId("transiD")
                .type("TYPE")
                .build();

        NotificationViewedInt notificationViewedInt = buildNotificationViewedInt(iun, recIndex, viewDate, null, raddInfo);

        //WHEN
        handler.handleViewNotificationRadd(notificationViewedInt).block();

        //THEN

        Mockito.verify(viewNotification).startVewNotificationProcess(
                Mockito.eq(notification),
                Mockito.eq(recipientInt),
                Mockito.eq(notificationViewedInt)
        );
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void handleAlreadyViewedNotification() {
        //GIVEN
        String iun = "test_iun";

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        Instant viewDate = Instant.now();
        int recIndex = 0;

        NotificationViewedInt notificationViewedInt = buildNotificationViewedInt(iun, recIndex, viewDate, null, null);
        //WHEN
        handler.handleViewNotificationDelivery(notificationViewedInt);

        //THEN
        Mockito.verify(viewNotification,  Mockito.never()).startVewNotificationProcess(
                Mockito.any(), 
                Mockito.any(),
                Mockito.any()
        );
    }

    private NotificationInt getNotification(String iun) {
        return NotificationInt.builder()
                .iun(iun)
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .internalId("testInternalId")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleCancellationRequested() {
        //GIVEN
        String iun = "test_iun_handleCancellationRequested";

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn (true);

        Instant viewDate = Instant.now();
        int recIndex = 0;

        //WHEN
        handler.handleViewNotificationDelivery(buildNotificationViewedInt(iun, recIndex, viewDate, null, null));

        //THEN
        Mockito.verify(notificationService,  Mockito.never()).getNotificationByIun(iun);
        Mockito.verify(timelineUtils,  Mockito.never()).checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt());
        Mockito.verify(viewNotification,  Mockito.never()).startVewNotificationProcess(
            Mockito.any(),
            Mockito.any(),
            Mockito.any()
        );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleCancellationNotRequested() {
        //GIVEN
        String iun = "test_iun_handleCancellationNotRequested";

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn (false);

        Instant viewDate = Instant.now();
        int recIndex = 0;

        //WHEN
        handler.handleViewNotificationDelivery(buildNotificationViewedInt(iun, recIndex, viewDate, null, null));

        //THEN
        Mockito.verify(timelineUtils,  Mockito.atLeastOnce()).checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt());

    }
    @ExtendWith(MockitoExtension.class)
    @Test
    void handleViewNotificationRaddRetrieved() {
        String iun = "test_iun_handleCancellationNotRequested";
        RequestNotificationViewedDto requestNotificationViewedDto = new RequestNotificationViewedDto();
        requestNotificationViewedDto.setRecipientInternalId("testInternalId");
        requestNotificationViewedDto.setRaddType("ALT");
        requestNotificationViewedDto.setRecipientType(RecipientType.PF);

        //WHEN
        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(getNotification(iun));
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(iun)).thenReturn(false);
        Mockito.doNothing().when(paperNotificationFailedService).deleteNotificationFailed(Mockito.any(), Mockito.any());
        Mockito.when(timelineService.addTimelineElement(Mockito.any(), Mockito.any())).thenReturn(true);

        StepVerifier.create(handler.handleNotificationRaddRetrieved(iun, requestNotificationViewedDto))
                .verifyComplete();

        //THEN
        Mockito.verify(paperNotificationFailedService).deleteNotificationFailed(Mockito.any(),Mockito.any());
        Mockito.verify(timelineService).addTimelineElement(Mockito.any(), Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleCancelledViewNotificationRaddRetrieved() {
        String iun = "test_iun_handleCancellationNotRequested";
        RequestNotificationViewedDto requestNotificationViewedDto = new RequestNotificationViewedDto();
        requestNotificationViewedDto.setRecipientInternalId("testInternalId");
        requestNotificationViewedDto.setRaddType("ALT");
        requestNotificationViewedDto.setRecipientType(RecipientType.PF);

        //WHEN
        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(getNotification(iun));
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(iun)).thenReturn(true);

        StepVerifier.create(handler.handleNotificationRaddRetrieved(iun, requestNotificationViewedDto))
                .verifyComplete();

        //THEN
        Mockito.verifyNoInteractions(paperNotificationFailedService, timelineService);
        Mockito.verify(timelineUtils, Mockito.times(0)).checkIsNotificationViewed(Mockito.any(), Mockito.any());
    }

    private NotificationViewedInt buildNotificationViewedInt(
            String iun,
            Integer recIndex,
            Instant viewedDate,
            DelegateInfoInt delegateInfo,
            RaddInfo raddInfo
    ) {
        return NotificationViewedInt.builder()
                .iun(iun)
                .recipientIndex(recIndex)
                .viewedDate(viewedDate)
                .raddInfo(raddInfo)
                .delegateInfo(delegateInfo)
                .build();
    }
}
