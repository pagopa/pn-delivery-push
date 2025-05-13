package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.nationalregistries.CheckTaxIdOKInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.PhysicalAddressesRequestBody;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.RecipientAddressRequestBody;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.nationalregistries.NationalRegistriesClient;
import it.pagopa.pn.deliverypush.service.NationalRegistriesService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.CheckTaxIdOK;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALID_PHYSICALADDRESS;

@Slf4j
@Service
public class NationalRegistriesServiceImpl implements NationalRegistriesService {
    private final PublicRegistryUtils publicRegistryUtils;
    private final NationalRegistriesClient nationalRegistriesClient;
    private final NotificationUtils notificationUtils;

    private final TimelineUtils timelineUtils;

    private final TimelineService timelineService;

    public NationalRegistriesServiceImpl(PublicRegistryUtils publicRegistryUtils,
                                         NationalRegistriesClient nationalRegistriesClient,
                                         NotificationUtils notificationUtils,
                                         TimelineService timelineService,
                                         TimelineUtils timelineUtils) {
        this.publicRegistryUtils = publicRegistryUtils;
        this.nationalRegistriesClient = nationalRegistriesClient;
        this.notificationUtils = notificationUtils;
        this.timelineUtils = timelineUtils;
        this.timelineService = timelineService;
    }

    /**
     * Send get request to public registry for get digital address
     **/
    @Override
    public void sendRequestForGetDigitalGeneralAddress(NotificationInt notification, 
                                                       Integer recIndex,
                                                       ContactPhaseInt contactPhase, 
                                                       int sentAttemptMade,
                                                       String relatedFeedbackTimelineId) {

        String correlationId = publicRegistryUtils.generateCorrelationId(notification.getIun(), recIndex, contactPhase, sentAttemptMade, DeliveryModeInt.DIGITAL);
        log.debug("Start Async Request for get general address, correlationId={} - iun={} id={}", correlationId, notification.getIun(), recIndex);

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);

        nationalRegistriesClient.sendRequestForGetDigitalAddress(recipient.getTaxId(), recipient.getRecipientType().getValue(), correlationId, notification.getSentAt());
        publicRegistryUtils.addPublicRegistryCallToTimeline(
                notification,
                recIndex,
                contactPhase,
                sentAttemptMade,
                correlationId, 
                DeliveryModeInt.DIGITAL, 
                relatedFeedbackTimelineId);

        log.debug("End sendRequestForGetAddress correlationId={} - iun={} id={}", correlationId, notification.getIun(), recIndex);
    }

    @Override
    public CheckTaxIdOKInt checkTaxId(String taxId) {
        log.info("Start checkTaxId for taxId={}", LogUtils.maskTaxId(taxId));

        CheckTaxIdOK response = nationalRegistriesClient.checkTaxId(taxId);

        return CheckTaxIdOKInt.builder()
                .taxId(taxId)
                .isValid(response.getIsValid())
                .errorCode(response.getErrorCode() != null ? response.getErrorCode().getValue() : null )
                .build();
    }

    @Override
    public List<NationalRegistriesResponse> getMultiplePhysicalAddress(NotificationInt notification) {
        log.info("Start getMultiplePhysicalAddress for notification iun={}", notification.getIun());

        String eventId = TimelineEventId.NATIONAL_REGISTRY_VALIDATION_CALL.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .deliveryMode(DeliveryModeInt.ANALOG)
                        .build());

        List<RecipientAddressRequestBody> recipientAddressRequests = buildRecipientAddressRequest(notification.getRecipients());
        if (CollectionUtils.isEmpty(recipientAddressRequests)) {
            throw new PnInternalException("No recipients to send request for get physical address", ERROR_CODE_DELIVERYPUSH_INVALID_PHYSICALADDRESS);
        }

        List<NationalRegistriesResponse> nationalRegistriesResponses = nationalRegistriesClient.sendRequestForGetPhysicalAddresses(buildRequestBody(recipientAddressRequests, eventId));

        List<Integer> recIndexList = extractRecipientsIndexesWithoutPhysicalAddress(recipientAddressRequests);
        TimelineElementInternal timelineElement = timelineUtils.buildNationalRegistryValidationCall(eventId, notification, recIndexList, DeliveryModeInt.ANALOG);
        timelineService.addTimelineElement(timelineElement, notification);

        return nationalRegistriesResponses;
    }

    private static PhysicalAddressesRequestBody buildRequestBody(List<RecipientAddressRequestBody> recipientAddressRequests, String eventId) {
        PhysicalAddressesRequestBody requestBody = new PhysicalAddressesRequestBody();
        requestBody.setAddresses(recipientAddressRequests);
        requestBody.setCorrelationId(eventId);
        requestBody.setReferenceRequestDate(Instant.now());
        return requestBody;
    }

    private static List<RecipientAddressRequestBody> buildRecipientAddressRequest(List<NotificationRecipientInt> recipients) {
        List<RecipientAddressRequestBody> addressRequestBody = new ArrayList<>();
        int recIndex = 0;

        for (NotificationRecipientInt recipient : recipients) {
            if (recipient.getPhysicalAddress() == null) {
                RecipientAddressRequestBody recipientAddressRequestBody = new RecipientAddressRequestBody();
                recipientAddressRequestBody.setTaxId(recipient.getTaxId());
                recipientAddressRequestBody.setRecipientType(RecipientAddressRequestBody.RecipientTypeEnum.fromValue(recipient.getRecipientType().getValue()));
                recipientAddressRequestBody.setRecIndex(recIndex);
                addressRequestBody.add(recipientAddressRequestBody);
            } else {
                log.debug("Recipient with index {} already has a physical address; It will not be used for LookupAddress service.", recIndex);
            }
            recIndex++;
        }
        return addressRequestBody;
    }

    private static List<Integer> extractRecipientsIndexesWithoutPhysicalAddress(List<RecipientAddressRequestBody> recipientAddressRequests) {
        List<Integer> indexesWithoutPhysicalAddress = new ArrayList<>();
        for (RecipientAddressRequestBody requestBody : recipientAddressRequests) {
            indexesWithoutPhysicalAddress.add(requestBody.getRecIndex());
        }
        return indexesWithoutPhysicalAddress;
    }

}
