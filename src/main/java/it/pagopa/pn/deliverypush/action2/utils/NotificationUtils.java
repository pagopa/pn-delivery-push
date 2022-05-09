package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.springframework.stereotype.Component;

@Component
public class NotificationUtils {

    public int getRecipientIndex(Notification notification, String taxId){
        int index = 0;

        for(NotificationRecipient recipientNot : notification.getRecipients()){
            if(recipientNot.getTaxId().equals(taxId)){
                return index;
            }
            index ++;
        }

        throw new PnInternalException("There isn't recipient in Notification");
    }

    public NotificationRecipient getRecipientFromIndex(Notification notification, int index){
        return notification.getRecipients().get(index);
    }
    
}
