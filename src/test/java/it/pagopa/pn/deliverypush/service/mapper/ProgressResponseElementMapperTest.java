package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.ProgressResponseElement;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.RefusedReasonEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

class ProgressResponseElementMapperTest {

    public static final String ERROR_CODE = "FILE_NOTFOUND";
    public static final String DETAIL = "Allegato non trovato. fileKey=81dde2a8-9719-4407-b7b3-63e7ea694869";

    @Test
    void internalToExternal() {

        RefusedReasonEntity refusedReasonEntity = new RefusedReasonEntity();
        refusedReasonEntity.setErrorCode( ERROR_CODE );
        refusedReasonEntity.setDetail( DETAIL );

        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.REQUEST_REFUSED.getValue());

        eventEntity.setValidationErrors( List.of(refusedReasonEntity) );
        ProgressResponseElement progressResponseElement = ProgressResponseElementMapper.internalToExternal(eventEntity);

        Assertions.assertEquals( ERROR_CODE, progressResponseElement.getValidationErrors().get( 0 ).getErrorCode() );
        Assertions.assertEquals( DETAIL, progressResponseElement.getValidationErrors().get( 0 ).getDetail() );
    }
}
