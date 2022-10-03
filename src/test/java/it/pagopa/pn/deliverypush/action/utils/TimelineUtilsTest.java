package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementDetailsEntity;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;

class TimelineUtilsTest {

    @Mock
    private InstantNowSupplier instantNowSupplier;

    @Mock
    private TimelineService timelineService;

    private TimelineUtils timelineUtils;

    @BeforeEach
    void setUp() {

        instantNowSupplier = Mockito.mock(InstantNowSupplier.class);
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = new TimelineUtils(instantNowSupplier, timelineService);
    }

    @Test
    void buildTimeline() {

        NotificationViewedDetailsInt detailsInt = new NotificationViewedDetailsInt();
        detailsInt.setRecIndex(0);
        detailsInt.setNotificationCost(100);
        TimelineElementInternal actual = timelineUtils.buildTimeline(buildNotificationInt(), TimelineElementCategoryInt.REQUEST_ACCEPTED, "001", buildTimelineElementDetailsInt());
        Assertions.assertEquals("001", actual.getIun());
        Assertions.assertEquals("001", actual.getElementId());
        Assertions.assertEquals("pa_02", actual.getPaId());
        Assertions.assertEquals(TimelineElementCategoryInt.REQUEST_ACCEPTED, actual.getCategory());
        Assertions.assertEquals(detailsInt, actual.getDetails());
    }

  

    private TimelineElementDetailsInt parseDetailsFromEntity(TimelineElementDetailsEntity entity, TimelineElementCategoryInt category) {
        return SmartMapper.mapToClass(entity, category.getDetailsJavaClass());
    }

    private TimelineElementDetailsInt buildTimelineElementDetailsInt() {
        return parseDetailsFromEntity(TimelineElementDetailsEntity.builder()
                .recIndex(0)
                .notificationCost(100)
                .build(), TimelineElementCategoryInt.NOTIFICATION_VIEWED);

    }

    private NotificationInt buildNotificationInt() {
        return NotificationInt.builder()
                .iun("001")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId("pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .build()
                ))
                .build();
    }
}