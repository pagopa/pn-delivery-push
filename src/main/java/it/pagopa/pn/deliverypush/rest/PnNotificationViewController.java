package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.action.NotificationViewedHandler;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.EventComunicationApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.RequestNotificationViewedDto;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponseNotificationViewedDto;
import it.pagopa.pn.deliverypush.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class PnNotificationViewController implements EventComunicationApi {

    private final NotificationService notificationService;
    private final NotificationUtils notificationUtils;
    private final NotificationViewedHandler notificationViewedHandler;


    public PnNotificationViewController(NotificationService notificationService, NotificationUtils notificationUtils, NotificationViewedHandler notificationViewedHandler) {
        this.notificationService = notificationService;
        this.notificationUtils = notificationUtils;
        this.notificationViewedHandler = notificationViewedHandler;
    }

    @Override
    public Mono<ResponseEntity<ResponseNotificationViewedDto>> notifyNotificationViewed(String iun, Mono<RequestNotificationViewedDto> requestNotificationViewedDto, final ServerWebExchange exchange) {
        return requestNotificationViewedDto.flatMap(request -> Mono.fromSupplier(() -> {
            log.info("Start notifyNotificationViewed - iun={} internalId={} raddTransactionId={} raddType={}", iun, request.getRecipientInternalId(), request.getRaddBusinessTransactionId(), request.getRaddType());
            try {
                // get notification from iun
                NotificationInt notification = notificationService.getNotificationByIun(iun);
                if (notification == null) {
                    log.debug("Notification not found - iun={}", iun);
                    return ResponseEntity.badRequest().build();
                }
                // get recipient index from internal id
                int recIndex = notificationUtils.getRecipientIndexFromInternalId(notification, request.getRecipientInternalId());
                // handle view event
                notificationViewedHandler.handleViewNotification(iun, recIndex, request.getRaddType(), request.getRaddBusinessTransactionId(), request.getRaddBusinessTransactionDate());
                // return iun
                log.info("End notifyNotificationViewed - iun={} internalId={} raddTransactionId={} raddType={}", iun, request.getRecipientInternalId(), request.getRaddBusinessTransactionId(), request.getRaddType());
                ResponseNotificationViewedDto response = ResponseNotificationViewedDto.builder().iun(iun).build();
                return ResponseEntity.ok(response);
            } catch (Exception exp) {
                log.error("Error during process", exp);
                return ResponseEntity.internalServerError().build();
            }
        }));
    }
}
