package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.address.*;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notificationpaid.NotificationPaidInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notificationviewed.NotificationViewedInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.*;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.AnalogDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.CategorizedAttachmentsResultInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendEventInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.io.IoSendMessageResultInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypush.dto.timeline.*;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.SendResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

import static it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId.*;
import static it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt.PAYMENT;


@Component
@Slf4j
public class TimelineUtils {

    private final InstantNowSupplier instantNowSupplier;
    private final TimelineService timelineService;
    private final NotificationProcessCostService notificationProcessCostService;

    public TimelineUtils(InstantNowSupplier instantNowSupplier,
                         TimelineService timelineService,
                         NotificationProcessCostService notificationProcessCostService) {
        this.instantNowSupplier = instantNowSupplier;
        this.timelineService = timelineService;
        this.notificationProcessCostService = notificationProcessCostService;
    }

    public TimelineElementInternal buildTimeline(NotificationInt notification,
                                                 TimelineElementCategoryInt category,
                                                 String elementId,
                                                 @NotNull TimelineElementDetailsInt details) {

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(notification, category, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildTimeline(NotificationInt notification,
                                                 TimelineElementCategoryInt category,
                                                 String elementId,
                                                 TimelineElementDetailsInt details,
                                                 TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder) {
        return timelineBuilder
                .iun(notification.getIun())
                .category(category)
                .timestamp(Instant.now())
                .elementId(elementId)
                .details(details)
                .paId(notification.getSender().getPaId())
                .notificationSentAt(notification.getSentAt())
                .build();
    }

    public TimelineElementInternal buildValidateF24RequestTimelineElement(NotificationInt notification) {
        log.debug("buildValidateF24RequestTimelineElement - iun={}", notification.getIun());

        String correlationId = TimelineEventId.VALIDATE_F24_REQUEST.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());

        ValidateF24Int details = ValidateF24Int.builder().build();
        return buildTimeline(notification, TimelineElementCategoryInt.VALIDATE_F24_REQUEST, correlationId, details);
    }

    public TimelineElementInternal buildValidatedF24TimelineElement(NotificationInt notification) {
        log.debug("buildValidatedF24TimelineElement - iun={}", notification.getIun());

        String correlationId = TimelineEventId.VALIDATED_F24.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());

        ValidatedF24DetailInt detail = ValidatedF24DetailInt.builder().build();
        return buildTimeline(notification, TimelineElementCategoryInt.VALIDATED_F24, correlationId, detail);
    }

    public TimelineElementInternal buildValidateAndNormalizeAddressTimelineElement(NotificationInt notification, String elementId) {
        log.debug("buildValidateAddressTimelineElement - iun={}", notification.getIun());

        ValidateNormalizeAddressDetailsInt details = ValidateNormalizeAddressDetailsInt.builder().build();
        return buildTimeline(notification, TimelineElementCategoryInt.VALIDATE_NORMALIZE_ADDRESSES_REQUEST, elementId, details);
    }

    public TimelineElementInternal buildAcceptedRequestTimelineElement(NotificationInt notification, String legalFactId) {
        log.debug("buildAcceptedRequestTimelineElement - iun={}", notification.getIun());

        String elementId = TimelineEventId.REQUEST_ACCEPTED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());

        NotificationRequestAcceptedDetailsInt details = NotificationRequestAcceptedDetailsInt.builder()
                .paProtocolNumber(notification.getPaProtocolNumber())
                .idempotenceToken(notification.getIdempotenceToken())
                .notificationRequestId(Base64.getEncoder().encodeToString(notification.getIun().getBytes()))
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(singleLegalFactId(legalFactId, LegalFactCategoryInt.SENDER_ACK));

        return buildTimeline(notification, TimelineElementCategoryInt.REQUEST_ACCEPTED, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildAvailabilitySourceTimelineElement(Integer recIndex, NotificationInt notification, DigitalAddressSourceInt source, boolean isAvailable,
                                                                          int sentAttemptMade) {
        log.debug("buildAvailabilitySourceTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.GET_ADDRESS.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(source)
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );

        GetAddressInfoDetailsInt details = GetAddressInfoDetailsInt.builder()
                .recIndex(recIndex)
                .digitalAddressSource(source)
                .isAvailable(isAvailable)
                .attemptDate(instantNowSupplier.get())
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.GET_ADDRESS, elementId, details);
    }


    public TimelineElementInternal buildDigitalFeedbackTimelineElement(String digitalDomicileTimelineId,
                                                                       NotificationInt notification,
                                                                       ResponseStatusInt status,
                                                                       int recIndex,
                                                                       ExtChannelDigitalSentResponseInt extChannelDigitalSentResponseInt,
                                                                       SendInformation digitalAddressFeedback,
                                                                       Boolean isFirstSentRetry) {
        log.debug("buildDigitaFeedbackTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .sentAttemptMade(digitalAddressFeedback.getRetryNumber())
                        .source(digitalAddressFeedback.getDigitalAddressSource())
                        .isFirstSendRetry(isFirstSentRetry)
                        .build()
        );

        DigitalMessageReferenceInt digitalMessageReference = extChannelDigitalSentResponseInt.getGeneratedMessage();

        SendDigitalFeedbackDetailsInt details = SendDigitalFeedbackDetailsInt.builder()
                .deliveryFailureCause(extChannelDigitalSentResponseInt.getEventDetails())
                .digitalAddress(digitalAddressFeedback.getDigitalAddress())
                .digitalAddressSource(digitalAddressFeedback.getDigitalAddressSource())
                .responseStatus(status)
                .deliveryDetailCode(extChannelDigitalSentResponseInt.getEventCode().getValue())
                .recIndex(recIndex)
                .notificationDate(digitalAddressFeedback.getEventTimestamp())
                .sendingReceipts(
                        (digitalMessageReference != null && digitalMessageReference.getId() != null) ?
                                Collections.singletonList(SendingReceipt.builder()
                                        .id(digitalMessageReference.getId())
                                        .system(digitalMessageReference.getSystem())
                                        .build())
                                : null
                )
                .requestTimelineId(digitalDomicileTimelineId)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds((digitalMessageReference != null && digitalMessageReference.getLocation() != null) ? singleLegalFactId(digitalMessageReference.getLocation(), LegalFactCategoryInt.PEC_RECEIPT) : null);

        return buildTimeline(notification, TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildDigitalProgressFeedbackTimelineElement(NotificationInt notification,
                                                                               int recIndex,
                                                                               EventCodeInt eventCode,
                                                                               boolean shouldRetry,
                                                                               DigitalMessageReferenceInt digitalMessageReference,
                                                                               int progressIndex,
                                                                               SendInformation digitalAddressFeedback) {
        log.debug("buildDigitalDeliveringProgressTimelineElement - IUN={} and id={} and progressIndex={}", notification.getIun(), recIndex, progressIndex);

        String elementId = TimelineEventId.SEND_DIGITAL_PROGRESS.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .sentAttemptMade(digitalAddressFeedback.getRetryNumber())
                        .source(digitalAddressFeedback.getDigitalAddressSource())
                        .isFirstSendRetry(digitalAddressFeedback.getIsFirstSendRetry())
                        .progressIndex(progressIndex)
                        .build()
        );

        SendDigitalProgressDetailsInt details = SendDigitalProgressDetailsInt.builder()
                .digitalAddress(digitalAddressFeedback.getDigitalAddress())
                .digitalAddressSource(digitalAddressFeedback.getDigitalAddressSource())
                .eventTimestamp(digitalAddressFeedback.getEventTimestamp()) // non si è potuto salvare in notificationDate perchè era già usato (erroneamente ora...) da un istant.now
                .retryNumber(digitalAddressFeedback.getRetryNumber())
                .recIndex(recIndex)
                .notificationDate(instantNowSupplier.get()) // di fatto non è più usato ora che il timestamp del timelinelement coincide con instant.now.
                .deliveryDetailCode(eventCode.getValue())
                .shouldRetry(shouldRetry)
                .sendingReceipts(
                        (digitalMessageReference != null && digitalMessageReference.getId() != null) ?
                                Collections.singletonList(SendingReceipt.builder()
                                        .id(digitalMessageReference.getId())
                                        .system(digitalMessageReference.getSystem())
                                        .build())
                                : null
                )
                .isFirstSendRetry(digitalAddressFeedback.getIsFirstSendRetry())
                .relatedFeedbackTimelineId(digitalAddressFeedback.getRelatedFeedbackTimelineId())
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds((digitalMessageReference != null && digitalMessageReference.getLocation() != null) ? singleLegalFactId(digitalMessageReference.getLocation(), LegalFactCategoryInt.PEC_RECEIPT) : null);

        return buildTimeline(notification, TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildProbableDateSchedulingAnalogTimelineElement(Integer recIndex, NotificationInt notification,
                                                                                    String eventId, Instant schedulingAnalogDate) {
        log.debug("buildProbableDateSchedulingAnalogTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        ProbableDateAnalogWorkflowDetailsInt details = ProbableDateAnalogWorkflowDetailsInt.builder()
                .recIndex(recIndex)
                .schedulingAnalogDate(schedulingAnalogDate)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE, eventId, details);
    }

    public TimelineElementInternal buildSendCourtesyMessageTimelineElement(Integer recIndex, NotificationInt notification, CourtesyDigitalAddressInt address,
                                                                           Instant sendDate, String eventId, IoSendMessageResultInt ioSendMessageResult) {
        log.debug("buildSendCourtesyMessageTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        SendCourtesyMessageDetailsInt details = SendCourtesyMessageDetailsInt.builder()
                .recIndex(recIndex)
                .digitalAddress(address)
                .sendDate(sendDate)
                .ioSendMessageResult(ioSendMessageResult)
                .build();


        return buildTimeline(notification, TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, eventId, details);
    }


    public TimelineElementInternal buildPrepareSimpleRegisteredLetterTimelineElement(Integer recIndex, NotificationInt notification, PhysicalAddressInt address,
                                                                                     String eventId) {
        log.debug("buildPrepareSimpleRegisteredLetterTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        SimpleRegisteredLetterDetailsInt details = SimpleRegisteredLetterDetailsInt.builder()
                .recIndex(recIndex)
                .physicalAddress(address)
                .foreignState(address.getForeignState())
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(notification, TimelineElementCategoryInt.PREPARE_SIMPLE_REGISTERED_LETTER, eventId, details, timelineBuilder);
    }

    public TimelineElementInternal buildSendSimpleRegisteredLetterTimelineElement(Integer recIndex, NotificationInt notification, PhysicalAddressInt address,
                                                                                  SendResponse sendResponse, String productType, String prepareRequestId,
                                                                                  List<String> replacedF24AttachmentUrls,
                                                                                  CategorizedAttachmentsResultInt categorizedAttachmentsResult) {
        log.debug("buildSendSimpleRegisteredLetterTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        SimpleRegisteredLetterDetailsInt details = SimpleRegisteredLetterDetailsInt.builder()
                .recIndex(recIndex)
                .physicalAddress(address)
                .foreignState(address.getForeignState())
                .analogCost(sendResponse.getAmount())
                .productType(productType)
                .numberOfPages(sendResponse.getNumberOfPages())
                .envelopeWeight(sendResponse.getEnvelopeWeight())
                .prepareRequestId(prepareRequestId)
                .f24Attachments(replacedF24AttachmentUrls)
                .categorizedAttachmentsResult(categorizedAttachmentsResult)
                .vat(notification.getVat())
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(notification, TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildSimpleRegisteredLetterProgressTimelineElement(NotificationInt notification,
                                                                                      List<AttachmentDetailsInt> attachments,
                                                                                      int progressIndex,
                                                                                      BaseRegisteredLetterDetailsInt sendPaperDetails,
                                                                                      SendEventInt sendEventInt,
                                                                                      String sendRequestId) {
        log.debug("buildSimpleRegisteredLetterProgressTimelineElement - iun={} and id={}", notification.getIun(), sendPaperDetails.getRecIndex());

        String elementId = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER_PROGRESS.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(sendPaperDetails.getRecIndex())
                        .progressIndex(progressIndex)
                        .build()
        );

        SimpleRegisteredLetterProgressDetailsInt details = SimpleRegisteredLetterProgressDetailsInt.builder()
                .recIndex(sendPaperDetails.getRecIndex())
                .deliveryFailureCause(sendEventInt.getDeliveryFailureCause())
                .deliveryDetailCode(sendEventInt.getStatusDetail())
                .notificationDate(sendEventInt.getStatusDateTime())
                .attachments(attachments)
                .sendRequestId(sendRequestId)
                .registeredLetterCode(sendEventInt.getRegisteredLetterCode())
                .build();

        List<LegalFactsIdInt> legalFactsListEntryIds = getLegalFactsIdList(attachments);

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(legalFactsListEntryIds);

        return buildTimeline(notification, TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER_PROGRESS, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildPrepareDigitalNotificationTimelineElement(NotificationInt notification, Integer recIndex,
                                                                                  LegalDigitalAddressInt digitalAddress, DigitalAddressSourceInt addressSource, int sentAttemptMade, Instant lastAttemptMade,
                                                                                  DigitalAddressSourceInt nextDigitalAddressSource, Instant nextLastAttemptMadeForSource, int nextSourceAttemptsMade,
                                                                                  String sourceTimelineId) {
        log.debug("buildPrepareDigitalNotificationTimelineElement - IUN={} and id={} sourceTimelineId={}", notification.getIun(), recIndex, sourceTimelineId);

        String elementId = TimelineEventId.PREPARE_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(nextDigitalAddressSource)
                        .sentAttemptMade(nextSourceAttemptsMade)
                        .relatedTimelineId(sourceTimelineId)    // nel caso di scheduling a 7gg, di fatto si ripetevano gli stessi argomenti. La discriminante è che il sourcetimelineId è diverso
                        .build());

        PrepareDigitalDetailsInt details = PrepareDigitalDetailsInt.builder()
                .recIndex(recIndex)
                .retryNumber(sentAttemptMade)
                .digitalAddress(digitalAddress)
                .digitalAddressSource(addressSource)
                .attemptDate(lastAttemptMade)
                .nextDigitalAddressSource(nextDigitalAddressSource)
                .nextLastAttemptMadeForSource(nextLastAttemptMadeForSource)
                .nextSourceAttemptsMade(nextSourceAttemptsMade)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.PREPARE_DIGITAL_DOMICILE, elementId, details);
    }

    public TimelineElementInternal buildSendDigitalNotificationTimelineElement(Integer recIndex,
                                                                               NotificationInt notification,
                                                                               SendInformation sendInformation,
                                                                               String eventId) {
        log.debug("buildSendDigitalNotificationTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        SendDigitalDetailsInt details = SendDigitalDetailsInt.builder()
                .recIndex(recIndex)
                .retryNumber(sendInformation.getRetryNumber())
                .digitalAddress(sendInformation.getDigitalAddress())
                .digitalAddressSource(sendInformation.getDigitalAddressSource())
                .isFirstSendRetry(sendInformation.getIsFirstSendRetry())
                .relatedFeedbackTimelineId(sendInformation.getRelatedFeedbackTimelineId())
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, eventId, details);
    }


    public TimelineElementInternal buildPrepareAnalogNotificationTimelineElement(PhysicalAddressInt paAddress, Integer recIndex, NotificationInt notification,
                                                                                 String relatedRequestId, int sentAttemptMade, String eventId, PhysicalAddressInt addressDiscovered) {
        log.debug("buildPrepareAnalogNotificationTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);
        ServiceLevelInt serviceLevel = notification.getPhysicalCommunicationType() != null ? ServiceLevelInt.valueOf(notification.getPhysicalCommunicationType().name()) : null;

        BaseAnalogDetailsInt details = BaseAnalogDetailsInt.builder()
                .recIndex(recIndex)
                .physicalAddress(relatedRequestId == null ? paAddress : addressDiscovered)
                .serviceLevel(serviceLevel)
                .sentAttemptMade(sentAttemptMade)
                .relatedRequestId(relatedRequestId)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(notification, TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE, eventId, details, timelineBuilder);
    }

    public TimelineElementInternal buildPrepareAnalogFailureTimelineElement(PhysicalAddressInt foundAddress, String prepareRequestId, String failureCause, Integer recIndex, NotificationInt notification) {
        log.debug("buildPrepareAnalogFailureTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.PREPARE_ANALOG_DOMICILE_FAILURE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        PrepareAnalogDomicileFailureDetailsInt details = PrepareAnalogDomicileFailureDetailsInt.builder()
                .recIndex(recIndex)
                .foundAddress(foundAddress)
                .failureCause(failureCause)
                .prepareRequestId(prepareRequestId)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(notification, TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE_FAILURE, elementId, details, timelineBuilder);
    }


    public TimelineElementInternal buildSendAnalogNotificationTimelineElement(PhysicalAddressInt address,
                                                                              Integer recIndex,
                                                                              NotificationInt notification,
                                                                              AnalogDtoInt analogDtoInfo,
                                                                              List<String> replacedF24AttachmentUrls,
                                                                              CategorizedAttachmentsResultInt categorizedAttachmentsResult) {
        SendResponse sendResponse = analogDtoInfo.getSendResponse();
        log.debug("buildSendAnalogNotificationTimelineElement - IUN={} and id={} analogCost={} relatedRequestId={} replacedF24AttachmentUrls={}", notification.getIun(), recIndex, sendResponse.getAmount(), analogDtoInfo.getRelatedRequestId(), replacedF24AttachmentUrls);
        ServiceLevelInt serviceLevel = notification.getPhysicalCommunicationType() != null ? ServiceLevelInt.valueOf(notification.getPhysicalCommunicationType().name()) : null;

        String elementId = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .sentAttemptMade(analogDtoInfo.getSentAttemptMade())
                        .build());

        SendAnalogDetailsInt details = SendAnalogDetailsInt.builder()
                .recIndex(recIndex)
                .physicalAddress(address)
                .serviceLevel(serviceLevel)
                .sentAttemptMade(analogDtoInfo.getSentAttemptMade())
                .relatedRequestId(analogDtoInfo.getRelatedRequestId())
                .analogCost(sendResponse.getAmount())
                .productType(analogDtoInfo.getProductType())
                .numberOfPages(sendResponse.getNumberOfPages())
                .envelopeWeight(sendResponse.getEnvelopeWeight())
                .f24Attachments(replacedF24AttachmentUrls)
                .categorizedAttachmentsResult(categorizedAttachmentsResult)
                .prepareRequestId(analogDtoInfo.getPrepareRequestId())
                .vat(notification.getVat())
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(notification, TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildSuccessDigitalWorkflowTimelineElement(NotificationInt notification, Integer recIndex, LegalDigitalAddressInt address,
                                                                              String legalFactId) {
        log.debug("buildSuccessDigitalWorkflowTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        DigitalSuccessWorkflowDetailsInt details = DigitalSuccessWorkflowDetailsInt.builder()
                .recIndex(recIndex)
                .digitalAddress(address)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(singleLegalFactId(legalFactId, LegalFactCategoryInt.DIGITAL_DELIVERY));

        return buildTimeline(notification, TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW, elementId,
                details, timelineBuilder);
    }

    public TimelineElementInternal buildFailureDigitalWorkflowTimelineElement(NotificationInt notification,
                                                                              Integer recIndex,
                                                                              String legalFactId) {
        log.debug("buildFailureDigitalWorkflowTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        DigitalFailureWorkflowDetailsInt details = DigitalFailureWorkflowDetailsInt.builder()
                .recIndex(recIndex)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(singleLegalFactId(legalFactId, LegalFactCategoryInt.DIGITAL_DELIVERY));

        return buildTimeline(notification, TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW, elementId, details, timelineBuilder);
    }


    public TimelineElementInternal buildSuccessAnalogWorkflowTimelineElement(NotificationInt notification, Integer recIndex, PhysicalAddressInt address) {
        log.debug("buildSuccessAnalogWorkflowTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.ANALOG_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        AnalogSuccessWorkflowDetailsInt details = AnalogSuccessWorkflowDetailsInt.builder()
                .recIndex(recIndex)
                .physicalAddress(address)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(notification, TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW, elementId,
                details, timelineBuilder);
    }

    public TimelineElementInternal buildAnalogWorkflowRecipientDeceasedTimelineElement(
            NotificationInt notification,
            Integer recIndex,
            Instant notificationDate,
            PhysicalAddressInt address,
            Integer notificationCost,
            Boolean addNotificationCost
    ) {

        log.debug("buildAnalogWorkflowRecipientDeceasedTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.ANALOG_WORKFLOW_RECIPIENT_DECEASED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        AnalogWorfklowRecipientDeceasedDetailsInt details = AnalogWorfklowRecipientDeceasedDetailsInt.builder()
                .recIndex(recIndex)
                .physicalAddress(address)
                .notificationDate(notificationDate)
                .build();

        if (Boolean.TRUE.equals(addNotificationCost)) {
            details.setNotificationCost(notificationCost);
        }

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(notification, TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildFailureAnalogWorkflowTimelineElement(NotificationInt notification, Integer recIndex, String generatedAarUrl) {
        log.debug("buildFailureAnalogWorkflowTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        AnalogFailureWorkflowDetailsInt details = AnalogFailureWorkflowDetailsInt.builder()
                .recIndex(recIndex)
                .generatedAarUrl(generatedAarUrl)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(notification, TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW, elementId,
                details, timelineBuilder);
    }


    public TimelineElementInternal buildPublicRegistryResponseCallTimelineElement(NotificationInt notification, Integer recIndex, NationalRegistriesResponse response) {
        log.debug("buildPublicRegistryResponseCallTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String eventId = TimelineEventId.NATIONAL_REGISTRY_RESPONSE.buildEventId(response.getCorrelationId());

        PublicRegistryResponseDetailsInt details = PublicRegistryResponseDetailsInt.builder()
                .recIndex(recIndex)
                .digitalAddress(response.getDigitalAddress())
                .physicalAddress(response.getPhysicalAddress())
                .requestTimelineId(response.getCorrelationId())
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, eventId, details);
    }


    public TimelineElementInternal buildPublicRegistryCallTimelineElement(NotificationInt notification, Integer recIndex, String eventId, DeliveryModeInt deliveryMode,
                                                                          ContactPhaseInt contactPhase, int sentAttemptMade,
                                                                          String relatedFeedbackTimelineId) {
        log.debug("buildPublicRegistryCallTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        PublicRegistryCallDetailsInt details = PublicRegistryCallDetailsInt.builder()
                .recIndex(recIndex)
                .contactPhase(contactPhase)
                .sentAttemptMade(sentAttemptMade)
                .deliveryMode(deliveryMode)
                .sendDate(instantNowSupplier.get())
                .relatedFeedbackTimelineId(relatedFeedbackTimelineId)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, eventId, details);
    }


    public TimelineElementInternal buildAnalogProgressTimelineElement(NotificationInt notification,
                                                                      List<AttachmentDetailsInt> attachments,
                                                                      int progressIndex,
                                                                      BaseAnalogDetailsInt sendPaperDetails,
                                                                      SendEventInt sendEventInt,
                                                                      String sendRequestId) {
        log.debug("buildAnalogProgressTimelineElement - iun={} and id={} progressIndex={}", notification.getIun(), sendPaperDetails.getRecIndex(), progressIndex);

        String elementId = TimelineEventId.SEND_ANALOG_PROGRESS.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(sendPaperDetails.getRecIndex())
                        .sentAttemptMade(sendPaperDetails.getSentAttemptMade())
                        .progressIndex(progressIndex)
                        .build()
        );

        SendAnalogProgressDetailsInt details = SendAnalogProgressDetailsInt.builder()
                .recIndex(sendPaperDetails.getRecIndex())
                .deliveryFailureCause(sendEventInt.getDeliveryFailureCause())
                .deliveryDetailCode(sendEventInt.getStatusDetail())
                .notificationDate(sendEventInt.getStatusDateTime())
                .attachments(attachments)
                .sendRequestId(sendRequestId)
                .registeredLetterCode(sendEventInt.getRegisteredLetterCode())
                .serviceLevel(sendPaperDetails.getServiceLevel())
                .build();

        List<LegalFactsIdInt> legalFactsListEntryIds = getLegalFactsIdList(attachments);

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(legalFactsListEntryIds);

        return buildTimeline(notification, TimelineElementCategoryInt.SEND_ANALOG_PROGRESS, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildAnalogSuccessAttemptTimelineElement(NotificationInt notification, List<AttachmentDetailsInt> attachments,
                                                                            BaseAnalogDetailsInt sendPaperDetails, SendEventInt sendEventInt,
                                                                            String sendRequestId) {
        log.debug("buildAnalogSuccessAttemptTimelineElement - iun={} and id={}", notification.getIun(), sendPaperDetails.getRecIndex());

        String elementId = TimelineEventId.SEND_ANALOG_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(sendPaperDetails.getRecIndex())
                        .sentAttemptMade(sendPaperDetails.getSentAttemptMade())
                        .build()
        );

        SendAnalogFeedbackDetailsInt details = SendAnalogFeedbackDetailsInt.builder()
                .recIndex(sendPaperDetails.getRecIndex())
                .physicalAddress(sendPaperDetails.getPhysicalAddress())
                .sentAttemptMade(sendPaperDetails.getSentAttemptMade())
                .serviceLevel(sendPaperDetails.getServiceLevel())
                .newAddress(sendEventInt.getDiscoveredAddress())
                .deliveryDetailCode(sendEventInt.getStatusDetail())
                .notificationDate(sendEventInt.getStatusDateTime())
                .deliveryFailureCause(sendEventInt.getDeliveryFailureCause())
                .responseStatus(ResponseStatusInt.OK)
                .attachments(attachments)
                .sendRequestId(sendRequestId)
                .registeredLetterCode(sendEventInt.getRegisteredLetterCode())
                .build();

        List<LegalFactsIdInt> legalFactsListEntryIds = getLegalFactsIdList(attachments);

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(legalFactsListEntryIds);

        return buildTimeline(notification, TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK, elementId, details, timelineBuilder);
    }

    private List<LegalFactsIdInt> getLegalFactsIdList(List<AttachmentDetailsInt> attachments) {
        List<LegalFactsIdInt> legalFactsListEntryIds;
        if (attachments != null) {
            legalFactsListEntryIds = attachments.stream()
                    .map(k -> LegalFactsIdInt.builder()
                            .key(k.getUrl())
                            .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                            .build()
                    ).toList();
        } else {
            legalFactsListEntryIds = Collections.emptyList();
        }
        return legalFactsListEntryIds;
    }

    public TimelineElementInternal buildAnalogFailureAttemptTimelineElement(NotificationInt notification, int sentAttemptMade, List<AttachmentDetailsInt> attachments,
                                                                            BaseAnalogDetailsInt sendPaperDetails, SendEventInt sendEventInt, String sendRequestId) {
        log.debug("buildAnalogFailureAttemptTimelineElement - iun={} and id={}", notification.getIun(), sendPaperDetails.getRecIndex());

        String elementId = TimelineEventId.SEND_ANALOG_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(sendPaperDetails.getRecIndex())
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );

        SendAnalogFeedbackDetailsInt details = SendAnalogFeedbackDetailsInt.builder()
                .recIndex(sendPaperDetails.getRecIndex())
                .physicalAddress(sendPaperDetails.getPhysicalAddress())
                .sentAttemptMade(sentAttemptMade)
                .serviceLevel(sendPaperDetails.getServiceLevel())
                .newAddress(sendEventInt.getDiscoveredAddress())
                .deliveryFailureCause(sendEventInt.getDeliveryFailureCause())
                .deliveryDetailCode(sendEventInt.getStatusDetail())
                .notificationDate(sendEventInt.getStatusDateTime())
                .requestTimelineId(elementId)
                .responseStatus(ResponseStatusInt.KO)
                .attachments(attachments)
                .sendRequestId(sendRequestId)
                .registeredLetterCode(sendEventInt.getRegisteredLetterCode())
                .build();

        List<LegalFactsIdInt> legalFactsListEntryIds = getLegalFactsIdList(attachments);

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(legalFactsListEntryIds);

        return buildTimeline(notification, TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildNotificationViewedTimelineElement(
            NotificationInt notification,
            Integer recIndex,
            String legalFactId,
            Integer notificationCost,
            RaddInfo raddInfo,
            DelegateInfoInt delegateInfo,
            Instant eventTimestamp) {
        log.debug("buildNotificationViewedTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        NotificationViewedDetailsInt details = NotificationViewedDetailsInt.builder()
                .recIndex(recIndex)
                .notificationCost(notificationCost)
                .eventTimestamp(eventTimestamp)
                .raddType(raddInfo != null ? raddInfo.getType() : null)
                .raddTransactionId(raddInfo != null ? raddInfo.getTransactionId() : null)
                .delegateInfo(delegateInfo)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(singleLegalFactId(legalFactId, LegalFactCategoryInt.RECIPIENT_ACCESS));

        return buildTimeline(notification, TimelineElementCategoryInt.NOTIFICATION_VIEWED, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildNotificationViewedLegalFactCreationRequestTimelineElement(
            NotificationInt notification,
            String legalFactId,
            NotificationViewedInt notificationViewed
    ) {
        log.debug("buildNotificationViewedLegalFactCreationRequestTimelineElement - iun={} and id={}", notification.getIun(), notificationViewed.getRecipientIndex());

        String elementId = TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(notificationViewed.getRecipientIndex())
                        .build());

        NotificationViewedCreationRequestDetailsInt details = NotificationViewedCreationRequestDetailsInt.builder()
                .recIndex(notificationViewed.getRecipientIndex())
                .legalFactId(legalFactId)
                .raddType(notificationViewed.getRaddInfo() != null ? notificationViewed.getRaddInfo().getType() : null)
                .raddTransactionId(notificationViewed.getRaddInfo() != null ? notificationViewed.getRaddInfo().getTransactionId() : null)
                .delegateInfo(notificationViewed.getDelegateInfo())
                .eventTimestamp(notificationViewed.getViewedDate())
                .sourceChannel(notificationViewed.getSourceChannel())
                .sourceChannelDetails(notificationViewed.getSourceChannelDetails())
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(notification, TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST, elementId,
                details, timelineBuilder);
    }


    public TimelineElementInternal buildCompletelyUnreachableTimelineElement(NotificationInt notification, Integer recIndex, String legalFactId, Instant legalFactGenerationDate) {
        log.debug("buildCompletelyUnreachableTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        CompletelyUnreachableDetailsInt details = CompletelyUnreachableDetailsInt.builder()
                .recIndex(recIndex)
                .legalFactGenerationDate(legalFactGenerationDate)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(singleLegalFactId(legalFactId, LegalFactCategoryInt.ANALOG_FAILURE_DELIVERY));

        return buildTimeline(notification, TimelineElementCategoryInt.COMPLETELY_UNREACHABLE, elementId,
                details, timelineBuilder);
    }

    public TimelineElementInternal buildScheduleDigitalWorkflowTimeline(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt lastAttemptInfo, Instant schedulingDate) {
        log.debug("buildScheduledActionTimeline - iun={} and id={}", notification.getIun(), recIndex);
        String elementId = TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(lastAttemptInfo.getDigitalAddressSource())
                        .sentAttemptMade(lastAttemptInfo.getSentAttemptMade())
                        .build());

        ScheduleDigitalWorkflowDetailsInt details = ScheduleDigitalWorkflowDetailsInt.builder()
                .recIndex(recIndex)
                .lastAttemptDate(lastAttemptInfo.getLastAttemptDate())
                .digitalAddress(lastAttemptInfo.getDigitalAddress())
                .digitalAddressSource(lastAttemptInfo.getDigitalAddressSource())
                .sentAttemptMade(lastAttemptInfo.getSentAttemptMade())
                .schedulingDate(schedulingDate)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.SCHEDULE_DIGITAL_WORKFLOW, elementId, details);
    }

    public TimelineElementInternal buildScheduleAnalogWorkflowTimeline(NotificationInt notification, Integer recIndex, Instant schedulingDate) {
        log.debug("buildScheduleAnalogWorkflowTimeline - iun={} and id={}", notification.getIun(), recIndex);
        String elementId = TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        ScheduleAnalogWorkflowDetailsInt details = ScheduleAnalogWorkflowDetailsInt.builder()
                .recIndex(recIndex)
                .schedulingDate(schedulingDate)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, elementId, details);
    }


    public TimelineElementInternal buildRefinementTimelineElement(NotificationInt notification, Integer recIndex, Integer notificationCost, Boolean addNotificationCost, Instant refinementDate) {
        log.debug("buildRefinementTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        RefinementDetailsInt details = RefinementDetailsInt.builder()
                .recIndex(recIndex)
                .eventTimestamp(refinementDate)
                .build();

        if (Boolean.TRUE.equals(addNotificationCost)) {
            details.setNotificationCost(notificationCost);
        }

        return buildTimeline(notification, TimelineElementCategoryInt.REFINEMENT, elementId, details);
    }

    public TimelineElementInternal buildScheduleRefinement(NotificationInt notification, Integer recIndex, Instant schedulingDate) {
        log.debug("buildScheduleRefinement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.SCHEDULE_REFINEMENT_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        ScheduleRefinementDetailsInt details = ScheduleRefinementDetailsInt.builder()
                .recIndex(recIndex)
                .schedulingDate(schedulingDate)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.SCHEDULE_REFINEMENT, elementId, details);
    }

    public TimelineElementInternal buildRefusedRequestTimelineElement(NotificationInt notification, List<NotificationRefusedErrorInt> errors) {
        log.debug("buildRefusedRequestTimelineElement - iun={}", notification.getIun());

        String elementId = TimelineEventId.REQUEST_REFUSED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());

        int numberOfRecipients = notification.getRecipients().size();

        RequestRefusedDetailsInt details = RequestRefusedDetailsInt.builder()
                .refusalReasons(errors)
                .numberOfRecipients(numberOfRecipients)
                .notificationCost(notificationProcessCostService.getSendFee() * numberOfRecipients)
                .paProtocolNumber(notification.getPaProtocolNumber())
                .idempotenceToken(notification.getIdempotenceToken())
                .notificationRequestId(Base64.getEncoder().encodeToString(notification.getIun().getBytes()))
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.REQUEST_REFUSED, elementId, details);
    }

    public TimelineElementInternal buildAarGenerationTimelineElement(NotificationInt notification, Integer recIndex, String legalFactId, Integer numberOfPages) {
        log.debug("buildAarGenerationTimelineElement - iun={}", notification.getIun());

        String elementId = TimelineEventId.AAR_GENERATION.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        AarGenerationDetailsInt details = AarGenerationDetailsInt.builder()
                .recIndex(recIndex)
                .generatedAarUrl(legalFactId)
                .numberOfPages(numberOfPages)
                .build();

        return buildTimeline(
                notification,
                TimelineElementCategoryInt.AAR_GENERATION,
                elementId,
                details
        );
    }

    public TimelineElementInternal buildNotHandledTimelineElement(NotificationInt notification, Integer recIndex,
                                                                  String reasonCode, String reason) {
        log.debug("buildNotHandledTimelineElement - iun={} id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.NOT_HANDLED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        NotHandledDetailsInt details = NotHandledDetailsInt.builder()
                .recIndex(recIndex)
                .reasonCode(reasonCode)
                .reason(reason)
                .build();

        return buildTimeline(
                notification,
                TimelineElementCategoryInt.NOT_HANDLED,
                elementId,
                details
        );
    }

    public TimelineElementInternal buildNotificationPaidTimelineElement(NotificationInt notification, NotificationPaidInt notificationPaidInt, String elementId) {
        log.debug("buildNotificationPaidTimelineElement: {}", notificationPaidInt);

        NotificationPaidDetailsInt details = NotificationPaidDetailsInt.builder()
                .recIndex(notificationPaidInt.getRecipientIdx())
                .recipientType(notificationPaidInt.getRecipientType().getValue())
                .eventTimestamp(notificationPaidInt.getPaymentDate())
                .amount(notificationPaidInt.getAmount())
                .creditorTaxId(notificationPaidInt.getCreditorTaxId())
                .noticeCode(notificationPaidInt.getNoticeCode())
                .paymentSourceChannel(notificationPaidInt.getPaymentSourceChannel())
                .uncertainPaymentDate(notificationPaidInt.isUncertainPaymentDate())
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(
                notification,
                PAYMENT,
                elementId,
                details,
                timelineBuilder
        );
    }

    public TimelineElementInternal buildSenderAckLegalFactCreationRequest(NotificationInt notification, String legalFactId) {
        log.debug("buildSenderAckLegalFactCreationRequest- iun={}", notification.getIun());

        String elementId = TimelineEventId.SENDERACK_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());

        SenderAckCreationRequestDetailsInt details = SenderAckCreationRequestDetailsInt.builder()
                .legalFactId(legalFactId)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(
                notification,
                TimelineElementCategoryInt.SENDER_ACK_CREATION_REQUEST,
                elementId,
                details,
                timelineBuilder
        );
    }

    public TimelineElementInternal buildNotificationCancelledLegalFactCreationRequest(NotificationInt notification, String legalFactId) {
        log.debug("buildNotificationCancelledLegalFactCreationRequest- iun={}", notification.getIun());

        String elementId = TimelineEventId.NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());

        NotificationCancelledDocumentCreationRequestDetailsInt details = NotificationCancelledDocumentCreationRequestDetailsInt.builder()
                .legalFactId(legalFactId)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(
                notification,
                TimelineElementCategoryInt.NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST,
                elementId,
                details,
                timelineBuilder
        );
    }

    public TimelineElementInternal buildAarCreationRequest(NotificationInt notification, int recIndex, PdfInfo pdfInfo) {
        log.debug("buildAarCreationRequest- iun={}", notification.getIun());

        String elementId = this.createEventIdForAarCreationRequest(notification.getIun(), recIndex);

        AarCreationRequestDetailsInt details = AarCreationRequestDetailsInt.builder()
                .recIndex(recIndex)
                .aarKey(pdfInfo.getKey())
                .numberOfPages(pdfInfo.getNumberOfPages())
                .aarTemplateType(pdfInfo.getAarTemplateType())
                .build();
                
        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(
                notification,
                TimelineElementCategoryInt.AAR_CREATION_REQUEST,
                elementId,
                details,
                timelineBuilder
        );
    }

    public String createEventIdForAarCreationRequest(String iun, Integer recIndex) {
        return TimelineEventId.AAR_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
    }

    public TimelineElementInternal buildDigitalDeliveryLegalFactCreationRequestTimelineElement(NotificationInt notification,
                                                                                               Integer recIndex,
                                                                                               EndWorkflowStatus status,
                                                                                               Instant completionWorkflowDate,
                                                                                               LegalDigitalAddressInt address,
                                                                                               String legalFactId) {
        log.debug("buildPecDeliveryWorkflowLegalFactCreationRequestTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.DIGITAL_DELIVERY_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        DigitalDeliveryCreationRequestDetailsInt details = DigitalDeliveryCreationRequestDetailsInt.builder()
                .recIndex(recIndex)
                .endWorkflowStatus(status)
                .completionWorkflowDate(completionWorkflowDate)
                .digitalAddress(address)
                .legalFactId(legalFactId)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(notification, TimelineElementCategoryInt.DIGITAL_DELIVERY_CREATION_REQUEST, elementId,
                details, timelineBuilder);
    }


    public TimelineElementInternal buildAnalogDeliveryFailedLegalFactCreationRequestTimelineElement(NotificationInt notification,
                                                                                                    Integer recIndex,
                                                                                                    EndWorkflowStatus status,
                                                                                                    Instant completionWorkflowDate,
                                                                                                    String legalFactId) {
        log.debug("buildAnalogDeliveryFailedLegalFactCreationRequestTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.COMPLETELY_UNREACHABLE_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        CompletelyUnreachableCreationRequestDetails details = CompletelyUnreachableCreationRequestDetails.builder()
                .recIndex(recIndex)
                .endWorkflowStatus(status)
                .completionWorkflowDate(completionWorkflowDate)
                .legalFactId(legalFactId)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(notification, TimelineElementCategoryInt.COMPLETELY_UNREACHABLE_CREATION_REQUEST, elementId,
                details, timelineBuilder);
    }

    public TimelineElementInternal buildNormalizedAddressTimelineElement(NotificationInt notification,
                                                                         Integer recIndex,
                                                                         PhysicalAddressInt oldAddress,
                                                                         PhysicalAddressInt normalizedAddress) {
        log.debug("buildNormalizedAddressTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.NORMALIZED_ADDRESS.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        NormalizedAddressDetailsInt details = NormalizedAddressDetailsInt.builder()
                .recIndex(recIndex)
                .oldAddress(oldAddress)
                .normalizedAddress(normalizedAddress)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.NORMALIZED_ADDRESS, elementId, details);
    }

    public TimelineElementInternal buildCancelRequestTimelineElement(NotificationInt notification) {
        log.debug("buildCancelRequestTimelineElement - IUN={}", notification.getIun());

        String elementId = NOTIFICATION_CANCELLATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());
        NotificationCancellationRequestDetailsInt details = NotificationCancellationRequestDetailsInt.builder().
                cancellationRequestId(UUID.randomUUID().toString()).
                build();
        return buildTimeline(notification, TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST, elementId, details);
    }

    public TimelineElementInternal buildCancelledTimelineElement(NotificationInt notification, String legalFactId) {
        log.debug("buildCancelRequestTimelineElement - IUN={}", notification.getIun());

        List<Integer> notRefined = notRefinedRecipientIndexes(notification);
        String elementId = TimelineEventId.NOTIFICATION_CANCELLED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());

        NotificationCancelledDetailsInt details = NotificationCancelledDetailsInt.builder().
                notificationCost(notificationProcessCostService.getSendFee() * notRefined.size()).
                notRefinedRecipientIndexes(notRefined).
                build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(singleLegalFactId(legalFactId, LegalFactCategoryInt.NOTIFICATION_CANCELLED));

        return buildTimeline(notification, TimelineElementCategoryInt.NOTIFICATION_CANCELLED, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildGeneratedF24TimelineElement(NotificationInt notification, int recipientIndex, List<String> f24Attachments) {
        log.debug("buildGeneratedF24TimelineElement - IUN={} and id={}", notification.getIun(), recipientIndex);

        String elementId = TimelineEventId.GENERATED_F24.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recipientIndex)
                        .build());
        GeneratedF24DetailsInt details = GeneratedF24DetailsInt.builder()
                .f24Attachments(f24Attachments)
                .recIndex(recipientIndex)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.GENERATED_F24, elementId, details);
    }

    public TimelineElementInternal buildGenerateF24RequestTimelineElement(NotificationInt notification) {
        log.debug("buildGenerateF24RequestTimelineElement - IUN={}", notification.getIun());

        String elementId = TimelineEventId.GENERATE_F24_REQUEST.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());

        GenerateF24Int details = GenerateF24Int.builder().build();
        return buildTimeline(notification, TimelineElementCategoryInt.GENERATE_F24_REQUEST, elementId, details);
    }

    public List<LegalFactsIdInt> singleLegalFactId(String legalFactKey, LegalFactCategoryInt type) {
        return Collections.singletonList(LegalFactsIdInt.builder()
                .key(legalFactKey)
                .category(type)
                .build());
    }

    public boolean checkNotificationIsViewedOrRefinedOrDeceasedOrCancelled(String iun, Integer recIndex) {
        log.debug("checkNotificationIsViewedOrRefinedOrDeceasedOrCancelled - iun={} recIndex={}", iun, recIndex);

        if (checkIsNotificationViewed(iun, recIndex)) {
            return true;
        }
        log.debug("Notification is not viewed need to check if it is refined - iun={} recIndex={}", iun, recIndex);

        if (checkIsNotificationRefined(iun, recIndex)) {
            return true;
        }
        log.debug("Notification is not refined need to check if it is deceased - iun={} recIndex={}", iun, recIndex);

        if (checkIsRecipientDeceased(iun, recIndex)) {
            return true;
        }
        log.debug("Notification is not deceased need to check if it is cancelled - iun={} recIndex={}", iun, recIndex);

        return checkIsNotificationCancellationRequested(iun);
    }

    public boolean checkIsNotificationPaid(String iun, Integer recIndex) {
        boolean isNotificationPaid = false;

        String elementId = TimelineEventId.NOTIFICATION_PAID.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        Set<TimelineElementInternal> notificationPaidElements = timelineService.getTimelineByIunTimelineId(iun, elementId, false);

        if (notificationPaidElements != null) {
            Optional<TimelineElementInternal> notificationPaidOpt = notificationPaidElements.stream().filter(element -> {
                        if (PAYMENT.equals(element.getCategory())) {
                            NotificationPaidDetailsInt details = (NotificationPaidDetailsInt) element.getDetails();
                            return recIndex.equals(details.getRecIndex());
                        }
                        return false;
                    }
            ).findFirst();

            isNotificationPaid = notificationPaidOpt.isPresent();
            log.debug("NotificationPaid value is={}", isNotificationPaid);
        }

        return isNotificationPaid;
    }

    public boolean checkIsNotificationViewed(String iun, Integer recIndex) {
        log.debug("checkNotificationIsAlreadyViewed - iun={} recIndex={}", iun, recIndex);

        Optional<TimelineElementInternal> notificationViewCreationRequestOpt = getNotificationViewCreationRequest(iun, recIndex);

        if (notificationViewCreationRequestOpt.isEmpty()) {
            log.debug("notificationViewCreationRequest is not present - iun={} recIndex={}", iun, recIndex);

            Optional<TimelineElementInternal> notificationViewOpt = getNotificationView(iun, recIndex);
            log.debug("notificationViewOpt is={} - iun={} recIndex={}", notificationViewOpt.isPresent(), iun, recIndex);

            return notificationViewOpt.isPresent();
        }

        return true;
    }


    public boolean checkIsNotificationRefined(String iun, Integer recIndex) {
        log.debug("checkIsNotificationRefined - iun={} recIndex={}", iun, recIndex);
        String elementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<TimelineElementInternal> notificationRefinedRequestOpt = timelineService.getTimelineElement(iun, elementId);

        log.debug("check notification refined is {} - iun={} recIndex={}", notificationRefinedRequestOpt.isPresent(), iun, recIndex);
        return notificationRefinedRequestOpt.isPresent();
    }

    public boolean checkIsRecipientDeceased(String iun, Integer recIndex) {
        log.debug("checkIsRecipientDeceased - iun={} recIndex={}", iun, recIndex);
        Optional<TimelineElementInternal> elementInternalOptional = getNotificationRecipientDeceased(iun, recIndex);

        log.debug("check recipient deceased is {} - iun={} recIndex={}", elementInternalOptional.isPresent(), iun, recIndex);
        return elementInternalOptional.isPresent();
    }

    public boolean checkIsNotificationCancellationRequested(String iun) {
        String elementId = NOTIFICATION_CANCELLATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        Set<TimelineElementInternal> notificationElements = timelineService.getTimelineByIunTimelineId(iun, elementId, false);

        boolean isNotificationCancelled = notificationElements != null && !notificationElements.isEmpty();
        log.debug("NotificationCancelled value is={}", isNotificationCancelled);

        return isNotificationCancelled;
    }

    public boolean checkIsNotificationCancelledLegalFactId(String iun, String legalFactId) {
        log.debug("Start checkIsNotificationCancelledLegalFactId - iun={} legalFactId={}", iun, legalFactId);
        
        String elementId = NOTIFICATION_CANCELLED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        Optional<TimelineElementInternal> notificationCancelledOpt = timelineService.getTimelineElement(iun, elementId);
        
        if(notificationCancelledOpt.isPresent()){
            TimelineElementInternal notificationCancelled = notificationCancelledOpt.get();
            return notificationCancelled.getLegalFactsIds().stream().anyMatch(legalFactsIdInt -> {
                String legalFactKeyReplaced = legalFactsIdInt.getKey().replace(PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX, "");
                return legalFactKeyReplaced.equals(legalFactId);
            });
        }
        
        return false;
    }

    public boolean checkIsNotificationRefused(String iun) {
        String elementId = REQUEST_REFUSED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        Set<TimelineElementInternal> notificationElements = timelineService.getTimelineByIunTimelineId(iun, elementId, false);

        boolean isNotificationRefused = notificationElements != null && !notificationElements.isEmpty();
        log.debug("NotificationRefused value is={}", isNotificationRefused);

        return isNotificationRefused;
    }

    public TimelineElementInternal buildNotificationRaddRetrieveTimelineElement(
            NotificationInt notification,
            Integer recIndex,
            RaddInfo raddInfo,
            Instant eventTimestamp) {
        log.debug("buildNotificationRaddRetrieveTimelineElement - iun={} and transactionId={}", notification.getIun(), raddInfo.getTransactionId());

        String elementId = TimelineEventId.NOTIFICATION_RADD_RETRIEVED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        NotificationRADDRetrievedDetailsInt details = NotificationRADDRetrievedDetailsInt.builder()
                .recIndex(recIndex)
                .eventTimestamp(eventTimestamp)
                .raddType(raddInfo.getType())
                .raddTransactionId(raddInfo.getTransactionId())
                .build();


        return buildTimeline(notification, TimelineElementCategoryInt.NOTIFICATION_RADD_RETRIEVED, elementId, details);
    }


    private List<Integer> notRefinedRecipientIndexes(NotificationInt notification) {
        log.debug("notRefinedRecipient - iun={} ", notification.getIun());
        List<Integer> notRefinedRecipientList = new ArrayList<>();
        int totRecipients = notification.getRecipients().size();
        for (int recIndex = 0; recIndex < totRecipients; recIndex++) {
            int notificationCost = 0;
            Optional<TimelineElementInternal> notificationOpt = getNotificationView(notification.getIun(), recIndex);
            if (notificationOpt.isPresent()) {
                NotificationViewedDetailsInt viewedDetailsInt = ((NotificationViewedDetailsInt) notificationOpt.get().getDetails());
                notificationCost = Optional.ofNullable(viewedDetailsInt.getNotificationCost()).orElse(0);
            }
            //If there is no notificationCost on View we check the Refinement
            if (notificationCost == 0) {
                notificationOpt = getNotificationRefinement(notification.getIun(), recIndex);
                if (notificationOpt.isPresent()) {
                    RefinementDetailsInt refinementDetailsInt = ((RefinementDetailsInt) notificationOpt.get().getDetails());
                    notificationCost = Optional.ofNullable(refinementDetailsInt.getNotificationCost()).orElse(0);
                }
            }
            //If there is no notificationCost on Refinement we check the Deceased
            if (notificationCost == 0) {
                notificationOpt = getNotificationRecipientDeceased(notification.getIun(), recIndex);
                if (notificationOpt.isPresent()) {
                    AnalogWorfklowRecipientDeceasedDetailsInt recipientDeceasedDetailsInt = ((AnalogWorfklowRecipientDeceasedDetailsInt) notificationOpt.get().getDetails());
                    notificationCost = Optional.ofNullable(recipientDeceasedDetailsInt.getNotificationCost()).orElse(0);
                }
            }

            if (notificationCost == 0) {
                notRefinedRecipientList.add(recIndex);
            }
        }

        return notRefinedRecipientList;
    }

    private Optional<TimelineElementInternal> getNotificationView(String iun, Integer recIndex) {
        String elementId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        return timelineService.getTimelineElement(iun, elementId);
    }

    public Optional<TimelineElementInternal> getNotificationViewCreationRequest(String iun, Integer recIndex) {
        String elementId = TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        return timelineService.getTimelineElement(iun, elementId);
    }

    public Optional<TimelineElementInternal> getScheduleRefinement(String iun, Integer recIndex) {
        String elementId = TimelineEventId.SCHEDULE_REFINEMENT_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        return timelineService.getTimelineElement(iun, elementId);
    }

    private Optional<TimelineElementInternal> getNotificationRefinement(String iun, Integer recIndex) {
        String elementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        return timelineService.getTimelineElement(iun, elementId);
    }

    private Optional<TimelineElementInternal> getNotificationRecipientDeceased(String iun, Integer recIndex) {
        String elementId = TimelineEventId.ANALOG_WORKFLOW_RECIPIENT_DECEASED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        return timelineService.getTimelineElement(iun, elementId);
    }

    public Optional<TimelineElementInternal> getValidatedF24(String iun){
        String elementId = TimelineEventId.VALIDATED_F24.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());
        return timelineService.getTimelineElement(iun, elementId);
    }

    public Optional<TimelineElementInternal> getGeneratedF24(String iun, Integer recIndex) {
        String elementId = TimelineEventId.GENERATED_F24.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        return timelineService.getTimelineElement(iun, elementId);
    }

    public TimelineElementInternal buildNationalRegistryValidationCall(String eventId, NotificationInt notification, List<Integer> recIndexes, DeliveryModeInt deliveryMode) {

        log.debug("buildNationalRegistryValidationCall - iun={}", notification.getIun());

        PublicRegistryValidationCallDetailsInt details = PublicRegistryValidationCallDetailsInt.builder()
                .recIndexes(recIndexes)
                .deliveryMode(deliveryMode)
                .sendDate(Instant.now())
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.PUBLIC_REGISTRY_VALIDATION_CALL, eventId, details);
    }

    public TimelineElementInternal buildNationalRegistryValidationResponse(NotificationInt notification, NationalRegistriesResponse response) {
        // TODO: Scommentare nel task di gestione validazione
        String eventId = TimelineEventId.NATIONAL_REGISTRY_VALIDATION_RESPONSE.buildEventId(
                EventId.builder()
                        .relatedTimelineId(response.getCorrelationId())
                        //.recIndex(response.getRecIndex())
                        .build());

        log.debug("buildNationalRegistryValidationResponse - iun={}", notification.getIun());

        PublicRegistryValidationResponseDetailsInt details = PublicRegistryValidationResponseDetailsInt.builder()
                //.recIndex(response.getRecIndex())
                //.registry(response.getRegistry())
                .physicalAddress(response.getPhysicalAddress())
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.PUBLIC_REGISTRY_VALIDATION_RESPONSE, eventId, details);
    }

    public String getIunFromTimelineId(String timelineId) {
        //<timelineId = CATEGORY_VALUE>;IUN_<IUN_VALUE>;RECINDEX_<RECINDEX_VALUE>...
        return timelineId.split("\\" + TimelineEventIdBuilder.DELIMITER)[1].replace("IUN_", "");
    }


}
