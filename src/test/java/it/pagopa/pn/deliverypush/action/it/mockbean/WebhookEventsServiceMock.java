package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDto;
import it.pagopa.pn.deliverypush.service.WebhookEventsService;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public class WebhookEventsServiceMock  implements WebhookEventsService {
    @Override
    public Mono<ProgressResponseElementDto> consumeEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId, String lastEventId) {
        return null;
    }

    @Override
    public Mono<Void> saveEvent(String paId, String eventId, String iun) {
        return null;
    }

    @Override
    public Mono<Void> purgeEvents(String streamId, String eventId, boolean olderThan) {
        return null;
    }
}
