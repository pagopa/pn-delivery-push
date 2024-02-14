package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
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
    private final NotificationViewedRequestHandler notificationViewedRequestHandler;


    public PnNotificationViewController(NotificationService notificationService, NotificationUtils notificationUtils, NotificationViewedRequestHandler notificationViewedRequestHandler) {
        this.notificationService = notificationService;
        this.notificationUtils = notificationUtils;
        this.notificationViewedRequestHandler = notificationViewedRequestHandler;
    }

    @Override
    public Mono<ResponseEntity<ResponseNotificationViewedDto>> notifyNotificationViewed(String iun, Mono<RequestNotificationViewedDto> requestNotificationViewedDto, final ServerWebExchange exchange) {
        return requestNotificationViewedDto.flatMap(request -> {
            log.info("Start notifyNotificationViewed - iun={} internalId={} raddTransactionId={} raddType={}", iun, request.getRecipientInternalId(), request.getRaddBusinessTransactionId(), request.getRaddType());
            return Mono.fromCallable(() -> notificationService.getNotificationByIun(iun))
                    .flatMap( notification -> {
                        int recIndex = notificationUtils.getRecipientIndexFromInternalId(notification, request.getRecipientInternalId());

                        RaddInfo raddInfo = RaddInfo.builder()
                                .type(request.getRaddType())
                                .transactionId(request.getRaddBusinessTransactionId())
                                .build();
                        return notificationViewedRequestHandler.handleViewNotificationRadd(iun, recIndex, raddInfo, request.getRaddBusinessTransactionDate())
                                .then(
                                        Mono.fromCallable(() -> {
                                            log.info("End notifyNotificationViewed - iun={} internalId={} raddTransactionId={} raddType={}", iun, request.getRecipientInternalId(), request.getRaddBusinessTransactionId(), request.getRaddType());
                                            ResponseNotificationViewedDto response = ResponseNotificationViewedDto.builder().iun(iun).build();
                                            return ResponseEntity.ok(response);
                                        })
                                );
                    });
        });
    }

    @Override
    public Mono<ResponseEntity<ResponseNotificationViewedDto>> notifyNotificationRaddRetrieved(String iun, Mono<RequestNotificationViewedDto> requestNotificationViewedDto,  final ServerWebExchange exchange) {
        return requestNotificationViewedDto.flatMap(request -> {
            log.info("Start notifyNotificationRaddRetrieved - iun={} internalId={} raddTransactionId={} raddType={}", iun, request.getRecipientInternalId(), request.getRaddBusinessTransactionId(), request.getRaddType());
            return notificationViewedRequestHandler.handleNotificationRaddRetrieved(iun, request)
                    .doOnSuccess(success -> log.info("End notifyNotificationRaddRetrieved - iun={} internalId={} raddTransactionId={} raddType={}", iun, request.getRecipientInternalId(), request.getRaddBusinessTransactionId(), request.getRaddType()))
                    .thenReturn(ResponseEntity.ok(ResponseNotificationViewedDto.builder().iun(iun).build()));
        });
    }
}
