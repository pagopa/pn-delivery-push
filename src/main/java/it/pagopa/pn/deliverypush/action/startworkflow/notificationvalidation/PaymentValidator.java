package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaIntMode;
import it.pagopa.pn.deliverypush.exceptions.PnValidationNotMatchingShaException;
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

                    List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients = getPaymentsInfoToUpdateAndValidate(notification);

                    int notificationBaseCost = notificationProcessCostService.getNotificationBaseCost(notification.getPaFee());
                    
                    if( !paymentsInfoForRecipients.isEmpty() ){
                        notificationProcessCostService.setNotificationStepCost(
                                notificationBaseCost,
                                notification.getIun(),
                                paymentsInfoForRecipients,
                                notification.getSentAt(),
                                startWorkflowInstant,
                                UpdateCostPhaseInt.VALIDATION
                                );
                    } else {
                      log.debug("There isn't pagoPaPayment to validate and update");  
                    }

                } else {
                    final String errorDetail = "There isn't paFee. In Async integration and DeliveryMode state the paFee are mandatory";
                    log.logCheckingOutcome(VALIDATE_PAYMENT_PROCESS, false, errorDetail);

                    throw new PnValidationNotMatchingShaException(errorDetail);

                }
            }else {
                log.info("No need to start validate payment process, notification is not in async mode");    
            }
        }else {
            log.info("No need to start validate payment process, notification is not in DELIVERY MODE");
        }
        
        log.logCheckingOutcome(VALIDATE_PAYMENT_PROCESS, true);
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
