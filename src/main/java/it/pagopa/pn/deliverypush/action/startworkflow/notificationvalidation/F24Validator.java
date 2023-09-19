package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.ValidateF24Request;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24.PnF24Client;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@CustomLog
public class F24Validator {

    private final PnF24Client pnF24Client;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    public Mono<Void> requestValidateF24(NotificationInt notification, ValidateF24Request validateF24Request){
       log.debug("Start  requestValidateF24 - iun={}" , validateF24Request.getSetId());

        String correlationId = TimelineEventId.VALIDATE_F24_REQUEST.buildEventId(
                EventId.builder()
                        .iun(validateF24Request.getSetId())
                        .build());

        return pnF24Client.validate(validateF24Request)
                .then(
                        Mono.fromCallable(() -> {
                           log.debug("Get validateF24 sync response - iun={}" , validateF24Request.getSetId());

                            timelineService.addTimelineElement(
                                    timelineUtils.buildValidateF24TimelineElement(notification, correlationId),
                                    notification
                            );
                            return null;
                        })
                );
    }
}
