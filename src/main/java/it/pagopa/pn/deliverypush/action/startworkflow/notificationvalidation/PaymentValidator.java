package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.PaymentUtils;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaIntMode;
import it.pagopa.pn.deliverypush.exceptions.PnPaymentUpdateRetryException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationPaymentException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@AllArgsConstructor
@CustomLog
public class PaymentValidator {
    private static final String VALIDATE_PAYMENT_PROCESS = "Validate payment";
    
    private final NotificationProcessCostService notificationProcessCostService;
    
    public void validatePayments(NotificationInt notification, Instant startWorkflowInstant){
        log.logChecking(VALIDATE_PAYMENT_PROCESS);
        
        if(NotificationFeePolicy.DELIVERY_MODE.equals(notification.getNotificationFeePolicy())){
            if(PagoPaIntMode.ASYNC.equals(notification.getPagoPaIntMode())){
                checkIsPresentPayments(notification);
                
                if(notification.getPaFee() != null){
                    startValidationAndUpdateFeeProcess(notification, startWorkflowInstant);
                    log.logCheckingOutcome(VALIDATE_PAYMENT_PROCESS, true);
                } else {
                    final String errorDetail = "There isn't paFee. In Async integration and DeliveryMode state the paFee are mandatory";
                    handleFailValidation(errorDetail);
                }
            }else {
                log.info("No need to start validate payment process, notification is not in async mode");    
            }
        }else {
            log.info("No need to start validate payment process, notification is not in DELIVERY MODE");
        }
        
    }

    private void checkIsPresentPayments(NotificationInt notification) {
        notification.getRecipients().forEach(recipient -> {
            int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
            log.debug("Start check is present payments - iun={} recIndex={}",notification.getIun(), recIndex);

            if(recipient.getPayments() == null || recipient.getPayments().isEmpty()){
                log.info("For recIndex={} is not present payment - iun={}", recIndex, notification.getIun());
                final String errorDetail = String.format(
                        "There isn't payments for recipient. With notificationFeePolicy=%s and pagoPaIntMode=%s payments are mandatory",
                        notification.getNotificationFeePolicy(),
                        notification.getPagoPaIntMode()
                );
                handleFailValidation(errorDetail);
            }
        });
    }

    private void startValidationAndUpdateFeeProcess(NotificationInt notification, Instant startWorkflowInstant) {
        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients = PaymentUtils.getPaymentsInfoFromNotification(notification);

        int notificationBaseCost = notificationProcessCostService.getNotificationBaseCost(notification.getPaFee());

        if( !paymentsInfoForRecipients.isEmpty() ){
            UpdateNotificationCostResponseInt updateNotificationCostResponse = notificationProcessCostService.setNotificationStepCost(
                    notificationBaseCost,
                    notification.getIun(),
                    paymentsInfoForRecipients,
                    notification.getSentAt(),
                    startWorkflowInstant,
                    UpdateCostPhaseInt.VALIDATION
                    ).block();
            
            if(updateNotificationCostResponse != null && !updateNotificationCostResponse.getUpdateResults().isEmpty()) {
                handleResponse(notification, updateNotificationCostResponse);
                
            } else {
                final String errorDetail = "Validation need to be rescheduled. Update notification response is not valid - iun=" + notification.getIun();
                handleRescheduleValidation(errorDetail);
            }
            
        } else {
          log.debug("There isn't pagoPaPayment to validate and update");  
        }
    }

    private static void handleResponse(NotificationInt notification, UpdateNotificationCostResponseInt updateNotificationCostResponse) {
        log.debug("Start handle update and validation cost response");
        
        updateNotificationCostResponse.getUpdateResults().forEach(response -> {
            PaymentsInfoForRecipientInt paymentsInfo = response.getPaymentsInfoForRecipient();
            
            log.debug("Start handle validation-update response for iun={} recIndex={} creditorTaxId={} noticeCode={}",
                    notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
            
            switch (response.getResult()) {
                case OK -> log.debug("Update and validation OK for iun={} recIndex={} creditorTaxId={} noticeCode={}",
                            notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                case KO -> {
                    final String errorDetail = String.format( "Payment information is not valid - creditorTaxId=%s noticeCode=%s",
                            paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                    handleFailValidation(errorDetail);
                }
                case RETRY -> {
                    final String errorDetail = String.format("Validation need to be rescheduled, can't have response from service. iun=%s recIndex=%s creditorTaxId=%s noticeCode=%s",
                            notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                    handleRescheduleValidation(errorDetail);
                }
                default -> {
                    final String errorDetail = String.format("Validation need to be rescheduled. Response received is not handled for iun=%s recIndex=%s creditorTaxId=%s noticeCode=%s",
                            notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                    handleRescheduleValidation(errorDetail);
                }
            }
        });
    }

    private static void handleRescheduleValidation(String errorDetail) {
        log.info(errorDetail);
        throw new PnPaymentUpdateRetryException(errorDetail);
    }

    private static void handleFailValidation(String errorDetail) {
        log.logCheckingOutcome(VALIDATE_PAYMENT_PROCESS, false, errorDetail);
        throw new PnValidationPaymentException(errorDetail);
    }

}
