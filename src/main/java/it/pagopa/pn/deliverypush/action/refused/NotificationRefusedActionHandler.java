package it.pagopa.pn.deliverypush.action.refused;

import it.pagopa.pn.deliverypush.action.utils.PaymentUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaIntMode;
import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnPaymentUpdateRetryException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@AllArgsConstructor
@CustomLog
public class NotificationRefusedActionHandler {
    public static final int NOTIFICATION_REFUSED_COST = 0;
    private final NotificationService notificationService;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    private final NotificationProcessCostService notificationProcessCostService;
    
    public void notificationRefusedHandler(String iun, List<NotificationRefusedErrorInt> errors, Instant schedulingTime){
        log.debug("Start notificationRefusedHandler - iun={}", iun);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        if(NotificationFeePolicy.DELIVERY_MODE.equals(notification.getNotificationFeePolicy()) &&
                PagoPaIntMode.ASYNC.equals(notification.getPagoPaIntMode())){
            handleUpdateNotificationCost(schedulingTime, notification);
        } else {
            log.debug("don't need to update notification cost - iun={}", iun);
        }
        
        addTimelineElement( timelineUtils.buildRefusedRequestTimelineElement(notification, errors), notification);
    }

    private void handleUpdateNotificationCost(Instant schedulingTime, NotificationInt notification) {
        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients = PaymentUtils.getPaymentsInfoFromNotification(notification);

        UpdateNotificationCostResponseInt updateNotificationCostResponse = notificationProcessCostService.setNotificationStepCost(
                NOTIFICATION_REFUSED_COST,
                notification.getIun(),
                paymentsInfoForRecipients,
                schedulingTime,
                schedulingTime,
                UpdateCostPhaseInt.REQUEST_REFUSED
        ).block();

        if (updateNotificationCostResponse != null && !updateNotificationCostResponse.getUpdateResults().isEmpty()) {
            handleResponse(notification, updateNotificationCostResponse);
        }
    }


    private static void handleResponse(NotificationInt notification, UpdateNotificationCostResponseInt updateNotificationCostResponse) {
        log.debug("Start handle update cost response {}", updateNotificationCostResponse.getIun());

        updateNotificationCostResponse.getUpdateResults().forEach(response -> {
            PaymentsInfoForRecipientInt paymentsInfo = response.getPaymentsInfoForRecipient();

            log.debug("Start handle update response for iun={} recIndex={} creditorTaxId={} noticeCode={}",
                    notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());

            switch (response.getResult()) {
                case OK -> log.debug("Update cost OK for iun={} recIndex={} creditorTaxId={} noticeCode={}",
                        notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                case KO -> log.error("Payment information is not valid. Can't update notification cost to={} for REQUEST_REFUSED" +
                            " - creditorTaxId={} noticeCode={}", NOTIFICATION_REFUSED_COST, paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                case RETRY -> {
                    final String errorDetail = String.format("Validation need to be rescheduled, can't have response from service. iun=%s recIndex=%s creditorTaxId=%s noticeCode=%s",
                            notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                    handleRetryError(errorDetail);
                }
                default -> {
                    final String errorDetail = String.format("Validation need to be rescheduled. Response received is not handled for iun=%s recIndex=%s creditorTaxId=%s noticeCode=%s",
                            notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                    handleRetryError(errorDetail);
                }
            }
        });
    }

    private static void handleRetryError(String errorDetail) {
        log.info(errorDetail);
        throw new PnPaymentUpdateRetryException(errorDetail);
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

}
