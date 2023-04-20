package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeItemsResultInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.NotificationRecipientAddressesDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class NormalizeAddressHandler {
    private final TimelineService timelineService;
    private final NotificationUtils notificationUtils;
    private final TimelineUtils timelineUtils;
    private final ConfidentialInformationService confidentialInformationService;
    
    public void handleNormalizedAddressResponse(NotificationInt notification, NormalizeItemsResultInt normalizeItemsResult){
        List<NotificationRecipientAddressesDtoInt> listNormalizedAddress = new ArrayList<>();
        
        normalizeItemsResult.getResultItems().forEach( result ->{
            int recIndex = Integer.parseInt(result.getId());
            NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);

            NotificationRecipientAddressesDtoInt normalizedAddressInt = NotificationRecipientAddressesDtoInt.builder()
                    .denomination(recipient.getDenomination())
                    .physicalAddress(result.getNormalizedAddress())
                    .build();
            listNormalizedAddress.add(normalizedAddressInt);
            
            timelineService.addTimelineElement(
                    timelineUtils.buildNormalizedAddressTimelineElement(notification, recIndex, recipient.getPhysicalAddress(), result.getNormalizedAddress()),
                    notification);
        });
        
        confidentialInformationService.updateNotificationAddresses(notification.getIun(), true, listNormalizedAddress);
    }
}
