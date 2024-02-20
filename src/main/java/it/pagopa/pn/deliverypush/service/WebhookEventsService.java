package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDto;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface WebhookEventsService {

    Mono<ProgressResponseElementDto> consumeEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId, String lastEventId);

    Mono<Void> saveEvent(String paId, String eventId, String iun);

    Mono<Void> purgeEvents(String streamId, String eventId, boolean olderThan);
}
