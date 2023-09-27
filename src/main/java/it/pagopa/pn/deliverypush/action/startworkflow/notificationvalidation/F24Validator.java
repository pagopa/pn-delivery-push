package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
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

    public Mono<Void> requestValidateF24(NotificationInt notification){
       log.debug("Start  requestValidateF24 - iun={}" , notification.getIun());

        return pnF24Client.validate(notification.getIun())
                .then(
                        Mono.fromCallable(() -> {
                           log.debug("Get validateF24 sync response - iun={}" , notification.getIun());
                            timelineService.addTimelineElement(
                                    timelineUtils.buildValidateF24RequestTimelineElement(notification),
                                    notification
                            );
                            return null;
                        })
                );
    }
}
