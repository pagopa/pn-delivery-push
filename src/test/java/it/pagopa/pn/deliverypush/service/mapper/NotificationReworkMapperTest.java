package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationReworkRequestInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkItem;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkError;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.InvalidateTimelineElementsRequest;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksErrorEntity;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestErrorCause;
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
        assertEquals(ReworkRequestType.INVALIDATE_ELEMENTS, internal.getRequestType());
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

    @Test
    void entityToExternal_mapsErrorListCorrectly() {
        NotificationReworksErrorEntity errorEntity = new NotificationReworksErrorEntity();
        errorEntity.setCause(ReworkRequestErrorCause.EXPIRED_ATTACHMENT);
        errorEntity.setDescription("Attachment expired");

        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setRequestType(ReworkRequestType.REWORK);
        entity.setErrors(List.of(errorEntity));

        List<ReworkItem> result = NotificationReworkMapper.entityToExternal(List.of(entity));

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getErrors().size());
        assertEquals(ReworkItem.RequestTypeEnum.REWORK, result.get(0).getRequestType());
        assertEquals(ReworkError.CauseEnum.EXPIRED_ATTACHMENT, result.get(0).getErrors().get(0).getCause());
        assertEquals("Attachment expired", result.get(0).getErrors().get(0).getDescription());
    }
}
