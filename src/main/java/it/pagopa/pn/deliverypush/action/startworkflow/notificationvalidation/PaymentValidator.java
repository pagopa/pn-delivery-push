package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaIntMode;
import it.pagopa.pn.deliverypush.exceptions.PnRescheduleValidationException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationPaymentException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
@CustomLog
public class PaymentValidator {
    private static final String VALIDATE_PAYMENT_PROCESS = "Validate payment";
    
    private final NotificationProcessCostService notificationProcessCostService;
    private final NotificationUtils notificationUtils;
    
    public void validatePayments(NotificationInt notification, Instant startWorkflowInstant){
        log.logChecking(VALIDATE_PAYMENT_PROCESS);
        
        if(NotificationFeePolicy.DELIVERY_MODE.equals(notification.getNotificationFeePolicy())){
            if(PagoPaIntMode.ASYNC.equals(notification.getPagoPaIntMode())){
                if(notification.getPaFee() != null){
                    startValidationAndUpdateFeeProcess(notification, startWorkflowInstant);
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
        
        log.logCheckingOutcome(VALIDATE_PAYMENT_PROCESS, true);
    }

    private void startValidationAndUpdateFeeProcess(NotificationInt notification, Instant startWorkflowInstant) {
        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients = getPaymentsInfoToUpdateAndValidate(notification);

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
        log.debug("Start handle update and validation cost response {}", updateNotificationCostResponse.getIun());
        
        updateNotificationCostResponse.getUpdateResults().forEach(response -> {
            PaymentsInfoForRecipientInt paymentsInfo = response.getPaymentsInfoForRecipient();
            
            log.debug("Start handle validation-update response for iun={} recIndex={} creditorTaxId={} noticeCode={}",
                    notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
            
            switch (response.getResult()) {
                case OK -> log.debug("Update and validation OK for iun={} recIndex={} creditorTaxId={} noticeCode={}",
                            notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                case KO -> {
                    final String errorDetail = String.format(
                            "Payment information is not valid - creditorTaxId=%s noticeCode=%s",
                            paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                    handleFailValidation(errorDetail);
                }
                case RETRY -> {
                    final String errorDetail = String.format(
                            "Validation need to be rescheduled, can't have response from service. iun=%s recIndex=%s creditorTaxId=%s noticeCode=%s",
                            notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                    handleRescheduleValidation(errorDetail);
                }
                default -> {
                    final String errorDetail = String.format(
                            "Validation need to be rescheduled. Response received is not handled for iun=%s recIndex=%s creditorTaxId=%s noticeCode=%s",
                            notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                    handleRescheduleValidation(errorDetail);
                }
            }
        });
    }

    private static void handleRescheduleValidation(String errorDetail) {
        log.info(errorDetail);
        throw new PnRescheduleValidationException(errorDetail);
    }

    private static void handleFailValidation(String errorDetail) {
        log.logCheckingOutcome(VALIDATE_PAYMENT_PROCESS, false, errorDetail);
        throw new PnValidationPaymentException(errorDetail);
    }

    @NotNull
    private List<PaymentsInfoForRecipientInt> getPaymentsInfoToUpdateAndValidate(NotificationInt notification) {
        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients = new ArrayList<>();

        notification.getRecipients().forEach(recipient -> {
            int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
            log.debug("Start add validation for recipient index {}", recIndex);
            
            recipient.getPayments().forEach( payment ->{
                final PagoPaInt pagoPaPayment = payment.getPagoPA();
                if(pagoPaPayment != null && Boolean.TRUE.equals(pagoPaPayment.getApplyCost())){
                    log.debug("Add validation for creditorTaxId={} noticeCode={} recIndex={}", pagoPaPayment.getCreditorTaxId(), pagoPaPayment.getNoticeCode(), recIndex);
                    PaymentsInfoForRecipientInt paymentsInfoForRecipient = PaymentsInfoForRecipientInt.builder()
                            .recIndex(recIndex)
                            .noticeCode(pagoPaPayment.getNoticeCode())
                            .creditorTaxId(pagoPaPayment.getCreditorTaxId())
                            .build();

                    paymentsInfoForRecipients.add(paymentsInfoForRecipient);
                }
            });
        });
        return paymentsInfoForRecipients;
    }
}
