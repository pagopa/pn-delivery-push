package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.GenerateF24Int;
import it.pagopa.pn.deliverypush.dto.timeline.details.GeneratedF24DetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.RequestAccepted;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24.PnF24Client;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static org.mockito.ArgumentMatchers.*;

public class F24ServiceImplTest {


    private PnF24Client pnF24Client;
    private NotificationProcessCostService notificationProcessCostService;
    private NotificationService notificationService;
    private TimelineUtils timelineUtils;
    private TimelineService timelineService;

    private F24ServiceImpl f24Service;

    @BeforeEach
    void setup() {
        pnF24Client = Mockito.mock(PnF24Client.class);
        notificationProcessCostService = Mockito.mock(NotificationProcessCostService.class);
        notificationService = Mockito.mock(NotificationService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        timelineService = Mockito.mock(TimelineService.class);
        f24Service = new F24ServiceImpl(pnF24Client,notificationProcessCostService,notificationService,timelineUtils,timelineService);
    }

    @Test
    void preparePDF() {
        String iun = "123";
        NotificationInt notification = getNotification(iun);
        RequestAccepted requestAccepted = new RequestAccepted();
        requestAccepted.setStatus("OK");
        TimelineElementInternal timelineElementInternal = buildF24RequestTimelineElement(notification);

        Mockito.when(notificationService.getNotificationByIun(any())).thenReturn(notification);

        Mockito.when(notificationProcessCostService.notificationProcessCostF24(anyString(),anyInt(),any(),anyInt(),anyInt(),anyString())).thenReturn(Mono.just(10));

        Mockito.when(timelineUtils.buildGenerateF24RequestTimelineElement(any())).thenReturn(timelineElementInternal);
        Mockito.when(pnF24Client.preparePDF(any(),any(),any())).thenReturn(Mono.just(requestAccepted));
        Mockito.when(timelineService.addTimelineElement(any(),any())).thenReturn(true);

        f24Service.preparePDF("123");

        Mockito.verify(notificationService,Mockito.times(1)).getNotificationByIun(eq(iun));
        Mockito.verify(timelineUtils,Mockito.times(1)).buildGenerateF24RequestTimelineElement(eq(notification));
        Mockito.verify(pnF24Client,Mockito.times(1)).preparePDF(eq(timelineElementInternal.getElementId()),eq(iun),eq(10));
        Mockito.verify(timelineService,Mockito.times(1)).addTimelineElement(eq(timelineElementInternal),eq(notification));
    }

    @Test
    void handleF24PrepareResponse() {
        String iun = "123";
        NotificationInt notification = getNotification(iun);
        TimelineElementInternal timelineElementInternal = buildF24RequestTimelineElement(notification);
        Map<Integer, List<String>> urls = Map.of(0,List.of("test"));

        Mockito.when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        Mockito.when(timelineUtils.buildGeneratedF24TimelineElement(any(),anyInt(),anyList())).thenReturn(timelineElementInternal);
        Mockito.when(timelineService.addTimelineElement(any(),any())).thenReturn(true);

        f24Service.handleF24PrepareResponse(iun,urls);

        Mockito.verify(notificationService,Mockito.times(1)).getNotificationByIun(eq(iun));
        Mockito.verify(timelineUtils,Mockito.times(1)).buildGeneratedF24TimelineElement(eq(notification),eq(0),eq(List.of("test")));
        Mockito.verify(timelineService,Mockito.times(1)).addTimelineElement(eq(timelineElementInternal),eq(notification));
    }



    private TimelineElementInternal buildGeneratedF24TimelineElement(NotificationInt notificationInt) {
        return TimelineElementInternal.builder()
                .iun(notificationInt.getIun())
                .elementId(TimelineEventId.GENERATED_F24.buildEventId(
                        EventId.builder()
                                .iun(notificationInt.getIun())
                                .recIndex(0)
                                .build()))
                .timestamp(Instant.now())
                .paId("77777777777")
                .category(TimelineElementCategoryInt.GENERATED_F24)
                .legalFactsIds(new ArrayList<>())
                .details(GeneratedF24DetailsInt.builder()
                        .f24Attachments(List.of("test"))
                        .recIndex(0)
                        .build())
                .build();
    }

    private TimelineElementInternal buildF24RequestTimelineElement(NotificationInt notificationInt) {
        return TimelineElementInternal.builder()
                .iun(notificationInt.getIun())
                .elementId(TimelineEventId.GENERATE_F24_REQUEST.buildEventId(
                        EventId.builder()
                                .iun(notificationInt.getIun())
                                .build()))
                .timestamp(Instant.now())
                .paId("77777777777")
                .category(TimelineElementCategoryInt.GENERATE_F24_REQUEST)
                .legalFactsIds(new ArrayList<>())
                .details(GenerateF24Int.builder().build())
                .build();
    }

    private NotificationInt getNotification(String iun) {
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("taxId")
                .withInternalId("ANON_"+"taxId")
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        return NotificationInt.builder()
                .iun(iun)
                .recipients(List.of(recipient))
                .vat(10)
                .paFee(10)
                .notificationFeePolicy(NotificationFeePolicy.FLAT_RATE)
                .version("23")
                .build();
    }

}
