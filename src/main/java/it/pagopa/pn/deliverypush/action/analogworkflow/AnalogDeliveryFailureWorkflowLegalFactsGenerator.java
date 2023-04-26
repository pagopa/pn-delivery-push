package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class AnalogDeliveryFailureWorkflowLegalFactsGenerator {

    private final SaveLegalFactsService saveLegalFactsService;
    private final NotificationUtils notificationUtils;

    public AnalogDeliveryFailureWorkflowLegalFactsGenerator(SaveLegalFactsService saveLegalFactsService,
                                                            NotificationUtils notificationUtils
    ) {
        this.saveLegalFactsService = saveLegalFactsService;
        this.notificationUtils = notificationUtils;
    }

    public String generateAndSendCreationRequestForAnalogDeliveryFailureWorkflowLegalFact(NotificationInt notification, Integer recIndex, EndWorkflowStatus status, Instant aarDate) {

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        return saveLegalFactsService.sendCreationRequestForAnalogDeliveryFailureWorkflowLegalFact(notification, recipient, status, aarDate);
    }
}
