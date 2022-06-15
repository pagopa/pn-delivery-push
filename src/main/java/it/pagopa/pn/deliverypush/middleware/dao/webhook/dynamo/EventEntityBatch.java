package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo;

import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import lombok.Data;

import java.util.List;

@Data
public class EventEntityBatch {
    private String streamId;
    private String lastEventIdRead;
    private List<EventEntity> events;
}
