package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationReworkRequestInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.InvalidateTimelineElementsRequest;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NotificationReworkMapperTest {

    @Test
    void externalToInternalInvalidateTimelineElements_mapsRequestCorrectly() {
        InvalidateTimelineElementsRequest request = new InvalidateTimelineElementsRequest();
        request.setRecIndex("RECINDEX_3");
        request.setTimelineElementIds(List.of("TL_1", "TL_2"));

        NotificationReworkRequestInternal internal = NotificationReworkMapper.externalToInternal(request, "IUN_123");

        assertEquals("IUN_123", internal.getIun());
        assertEquals("RECINDEX_3", internal.getRecIndex());
        assertEquals(List.of("TL_1", "TL_2"), internal.getElementsToInvalidate());
        assertEquals(ReworkRequestType.INVALIDATE_ELEMENTS, internal.getReworkRequestType());
        assertNull(internal.getAttemptId());
        assertNull(internal.getPcRetry());
        assertNull(internal.getReason());
    }

    @Test
    void externalToInternalInvalidateTimelineElements_defaultsRecIndexWhenMissing() {
        InvalidateTimelineElementsRequest request = new InvalidateTimelineElementsRequest();
        request.setTimelineElementIds(List.of("TL_1"));

        NotificationReworkRequestInternal internal = NotificationReworkMapper.externalToInternal(request, "IUN_123");

        assertEquals("RECINDEX_0", internal.getRecIndex());
    }
}
