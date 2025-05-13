package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@AllArgsConstructor
@Getter
@Component
@Slf4j
public class RefusalCostCalculator {
    private final PnTechnicalRefusalCostMode pnTechnicalRefusalCostMode;
    private final NotificationProcessCostService notificationProcessCostService;
    private final String ERROR_MESSAGE = "Invalid number of recipients not affected by technical errors";
    private final String ERROR_CODE = "INVALID_NUMBER_OF_RECIPIENTS";

    public int calculateRefusalCost(NotificationInt notification, List<NotificationRefusedErrorInt> errors) {
        // Numero totale di destinatari della notifica
        int numOfRecipients = notification.getRecipients().size();
        int numOfRecipientsAffectedFromTechnicalError = countRecipientsWithTechnicalErrors(errors, notification.getIun());

        // Se non ci sono destinatari legati a errori tecnici, applica la logica attuale
        if (numOfRecipientsAffectedFromTechnicalError == 0) {
            return numOfRecipients * notificationProcessCostService.getSendFee();
        }

        // Calcolo del costo di rifiuto della notifica in base alla modalità di costo del rifiuto tecnico
        return switch (pnTechnicalRefusalCostMode.getMode()) {
            case UNIFORM -> pnTechnicalRefusalCostMode.getCost();
            case RECIPIENT_BASED -> {
                int numOfRecipientsNotAffectedFromTechnicalError = numOfRecipients - numOfRecipientsAffectedFromTechnicalError;
                if (numOfRecipientsNotAffectedFromTechnicalError < 0) {
                    throw new PnInternalException(ERROR_MESSAGE, ERROR_CODE);
                }
                yield (numOfRecipientsAffectedFromTechnicalError * pnTechnicalRefusalCostMode.getCost()) +
                        (numOfRecipientsNotAffectedFromTechnicalError * notificationProcessCostService.getSendFee());
            }
        };
    }

    private int countRecipientsWithTechnicalErrors(List<NotificationRefusedErrorInt> errors, String iun) {
        // Calcolo del numero di destinatari che sono legati ad un errore che produce un rifiuto tecnico
        int count = 0;
        for (NotificationRefusedErrorInt error : errors) {
            PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt errorCode =
                    PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.fromValue(error.getErrorCode());
            if (errorCode != null && errorCode.getIsTechnicalRefusal()) {
                log.info("Notification with iun {} and recipient with index {} is affected by a technical error: {}", iun, error.getRecIndex(), error.getErrorCode());
                count++;
            }
        }
        return count;
    }

}
