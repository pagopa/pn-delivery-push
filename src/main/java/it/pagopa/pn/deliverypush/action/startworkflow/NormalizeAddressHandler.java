package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeItemsResultInt;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeResultInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.NotificationRecipientAddressesDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
@CustomLog
public class NormalizeAddressHandler {
    private final TimelineService timelineService;
    private final NotificationUtils notificationUtils;
    private final TimelineUtils timelineUtils;
    private final ConfidentialInformationService confidentialInformationService;
    
    public void handleNormalizedAddressResponse(NotificationInt notification, NormalizeItemsResultInt normalizeItemsResult){
        log.debug("Start handleNormalizedAddressResponse - iun={}", notification.getIun());

        List<NotificationRecipientAddressesDtoInt> listNormalizedAddress = new ArrayList<>();
        
        normalizeItemsResult.getResultItems().forEach( result ->{
            int recIndex = Integer.parseInt(result.getId());
            NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);

            if(result.getNormalizedAddress() != null){
                addNormalizeAddress(notification, listNormalizedAddress, result, recIndex, recipient);
            }
        });
        
        if( listNormalizedAddress.size() == notification.getRecipients().size() ){
            log.debug("Update confidential information with normalize address- iun={}", notification.getIun());
            confidentialInformationService.updateNotificationAddresses(notification.getIun(), true, listNormalizedAddress).block();
        } else {
            handleError(notification, listNormalizedAddress);
        }

        log.info("Ending validate and normalize address Process - iun={}", notification.getIun());
    }

    private static void handleError(NotificationInt notification, List<NotificationRecipientAddressesDtoInt> listNormalizedAddress) {
        String errorMsg = String.format(
                "Normalize address size=%d are different from recipientSize=%d - iun=%s",
                listNormalizedAddress.size(),
                notification.getRecipients().size(),
                notification.getIun()
        );
        log.fatal(errorMsg);
        throw new PnInternalException(errorMsg, PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NORMALIZE_ADDRESS_ERROR);
    }

    private void addNormalizeAddress(NotificationInt notification, List<NotificationRecipientAddressesDtoInt> listNormalizedAddress, NormalizeResultInt result, int recIndex, NotificationRecipientInt recipient) {
        log.debug("Normalized address for id={} - iun={}", recIndex, notification.getIun());

        PhysicalAddressInt normalizedAddressEnriched = enrichNormalizeAddress(result, recipient);

        NotificationRecipientAddressesDtoInt normalizedAddressInt = NotificationRecipientAddressesDtoInt.builder()
                .denomination(recipient.getDenomination())
                .digitalAddress(recipient.getDigitalDomicile())
                .physicalAddress(normalizedAddressEnriched)
                .build();
        listNormalizedAddress.add(normalizedAddressInt);

        timelineService.addTimelineElement(
                timelineUtils.buildNormalizedAddressTimelineElement(notification, recIndex, recipient.getPhysicalAddress(), result.getNormalizedAddress()),
                notification);
    }

    private PhysicalAddressInt enrichNormalizeAddress(NormalizeResultInt result, NotificationRecipientInt recipient) {
        PhysicalAddressInt physicalAddressInt = result.getNormalizedAddress();
        physicalAddressInt.setAt(recipient.getPhysicalAddress().getAt());
        physicalAddressInt.setFullname(recipient.getDenomination());
        return physicalAddressInt;
    }
}
