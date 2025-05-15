package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.datavault.NotificationRecipientAddressesDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.exceptions.PnLookupAddressValidationFailedException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.NationalRegistriesService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_LOOKUPADDRESS_INCONSISTENT_DATA;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.ADDRESS_NOT_FOUND;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.ADDRESS_SEARCH_FAILED;

@Component
@AllArgsConstructor
@CustomLog
public class LookupAddressHandler {

    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final ConfidentialInformationService confidentialInformationService;
    private final NationalRegistriesService nationalRegistriesService;

    public void performValidation(NotificationInt notification) {
        List<NationalRegistriesResponse> responses = nationalRegistriesService.getMultiplePhysicalAddress(notification);
        validateAddresses(responses);
        saveAddresses(responses, notification);
    }

    private void validateAddresses(List<NationalRegistriesResponse> responses) {
        log.info("Validating {} addresses retrieved by national registries", responses.size());
        List<ProblemError> errors = new ArrayList<>();
        for (NationalRegistriesResponse response : responses) {
            /*
             Effettuo prima il controllo sulla presenza di errori, perchè se ci sono errori non ci sono nemmeno indirizzi, ma in questo caso è giusto riportare solo l'errore
             */
            if (StringUtils.isNotBlank(response.getError())) {
                errors.add(
                        ProblemError.builder()
                                .code(ADDRESS_SEARCH_FAILED.getValue())
                                .element(response.getRecIndex().toString())
                                .detail("Address search for recipient index: " + response.getRecIndex() + ", encountered an error")
                                .build()
                );
            } else if (response.getPhysicalAddress() == null) {
                errors.add(
                        ProblemError.builder()
                                .code(ADDRESS_NOT_FOUND.getValue())
                                .element(response.getRecIndex().toString())
                                .detail("Address not found for recipient index: " + response.getRecIndex())
                                .build()
                );
            }

        }
        if (!errors.isEmpty()) {
            throw new PnLookupAddressValidationFailedException(errors);
        }
    }

    private void saveAddresses(List<NationalRegistriesResponse> responses, NotificationInt notification) {
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

            enrichAddressWithRecipientData(response, recipient);

            NotificationRecipientAddressesDtoInt foundAddress = NotificationRecipientAddressesDtoInt.builder()
                    .denomination(recipient.getDenomination())
                    .digitalAddress(recipient.getDigitalDomicile() != null ? recipient.getDigitalDomicile() : null)
                    .physicalAddress(response.getPhysicalAddress())
                    .recIndex(response.getRecIndex())
                    .build();
            recipientAddressesDtoList.add(foundAddress);
        }

        MDCUtils.addMDCToContextAndExecute(
                confidentialInformationService.updateNotificationAddresses(notification.getIun(), false, recipientAddressesDtoList)
        ).block();
    }

    private void enrichAddressWithRecipientData(NationalRegistriesResponse response, NotificationRecipientInt recipient) {
        response.getPhysicalAddress().setFullname(recipient.getDenomination());
    }

}
