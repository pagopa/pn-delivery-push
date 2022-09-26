package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONRECIPIENTNOTFOUND;

@Component
public class NotificationUtils {

    public int getRecipientIndex(NotificationInt notification, String taxId) {
        int index = 0;

        for (NotificationRecipientInt recipientNot : notification.getRecipients()) {
            if (recipientNot.getTaxId().equals(taxId)) {
                return index;
            }
            index++;
        }

        throw new PnInternalException("There isn't recipient in Notification", ERROR_CODE_DELIVERYPUSH_NOTIFICATIONRECIPIENTNOTFOUND);
    }

    public NotificationRecipientInt getRecipientFromIndex(NotificationInt notification, int index) {
        return notification.getRecipients().get(index);
    }

}
