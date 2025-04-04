package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.datavault.NotificationRecipientAddressesDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.exceptions.PnLookupAddressNotFoundException;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_LOOKUPADDRESS_INCONSISTENT_DATA;

@Component
@AllArgsConstructor
@CustomLog
public class LookupAddressHandler {

    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final ConfidentialInformationService confidentialInformationService;

    public void validateAddresses(List<NationalRegistriesResponse> responses) {
        log.info("Validating {} addresses retrieved by national registries", responses.size());
        List<String> errorMessages = new ArrayList<>();
        for (NationalRegistriesResponse response : responses) {
            if (response.getPhysicalAddress() == null) {
                errorMessages.add("Address not found for recipient index: " + response.getRecIndex());
            }
        }
        if (!errorMessages.isEmpty()) {
            throw new PnLookupAddressNotFoundException(errorMessages);
        }
    }

    public void saveAddresses(List<NationalRegistriesResponse> responses, NotificationInt notification) {
        log.info("Saving addresses for notification with IUN: {}", notification.getIun());
        List<NotificationRecipientAddressesDtoInt> recipientAddressesDtoList = new ArrayList<>();
        for (NationalRegistriesResponse response : responses) {
            timelineService.addTimelineElement(
                    timelineUtils.buildNationalRegistryValidationResponse(notification, response),
                    notification
            );
            NotificationRecipientInt recipient = notification.getRecipients().get(response.getRecIndex());

            if (recipient == null) {
                throw new PnInternalException(String.format("Recipient with recIndex %s not found in NotificationInt", response.getRecIndex()), ERROR_CODE_DELIVERYPUSH_LOOKUPADDRESS_INCONSISTENT_DATA);
            }

            NotificationRecipientAddressesDtoInt foundAddress = NotificationRecipientAddressesDtoInt.builder()
                    .denomination(recipient.getDenomination())
                    .digitalAddress(recipient.getDigitalDomicile() != null ? recipient.getDigitalDomicile() : null)
                    .physicalAddress(response.getPhysicalAddress())
                    .recIndex(response.getRecIndex())
                    .build();
            recipientAddressesDtoList.add(foundAddress);
        }
        confidentialInformationService.updateNotificationAddresses(notification.getIun(), false,recipientAddressesDtoList);
    }

}
