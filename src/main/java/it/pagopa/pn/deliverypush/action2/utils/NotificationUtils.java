package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.springframework.stereotype.Component;

@Component
public class NotificationUtils {

    public int getRecipientIndex(NotificationInt notification, String taxId){
        int index = 0;

        for(NotificationRecipientInt recipientNot : notification.getRecipients()){
            if(recipientNot.getTaxId().equals(taxId)){
                return index;
            }
            index ++;
        }

        throw new PnInternalException("There isn't recipient in Notification");
    }

    public NotificationRecipientInt getRecipientFromIndex(NotificationInt notification, int index){
        return notification.getRecipients().get(index);
    }
    
}
