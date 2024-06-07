package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND;

@Component
@AllArgsConstructor
@Slf4j
public class AarUtils {
    public static final String START_DYNAMIC_PROPERTY_CHARACTER = "<";
    public static final String END_DYNAMIC_PROPERTY_CHARACTER = ">";
    public static final String RADD_DYNAMIC_TEMPLATE_VALUE = "RADD_TEMPLATE_DEFINITION";

    private final SaveLegalFactsService saveLegalFactsService;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    private final NotificationUtils notificationUtils;
    private final DocumentCreationRequestService documentCreationRequestService;
    
    public void generateAARAndSaveInSafeStorageAndAddTimelineEvent(NotificationInt notification, Integer recIndex, String quickAccessToken) {
        try {
            // check se già è stato creato l'AAR, dunque se siamo in questo metodo a valle di un reinserimento in coda del messaggio
            String elementId = TimelineEventId.AAR_GENERATION.buildEventId(
                    EventId.builder()
                            .iun(notification.getIun())
                            .recIndex(recIndex)
                            .build());

            Optional<TimelineElementInternal> timeline = timelineService.getTimelineElement(notification.getIun(), elementId);
            if (timeline.isEmpty()) {
                PdfInfo pdfInfo = saveLegalFactsService.sendCreationRequestForAAR(notification, notificationUtils.getRecipientFromIndex(notification, recIndex), quickAccessToken);
                
                //Viene salvata in timeline la request document creation request
                TimelineElementInternal timelineElementInternal = timelineUtils.buildAarCreationRequest(notification, recIndex, pdfInfo);
                timelineService.addTimelineElement( timelineElementInternal , notification);

                //Vengono inserite le informazioni della richiesta di creazione del legalFacts a safeStorage
                documentCreationRequestService.addDocumentCreationRequest(pdfInfo.getKey(), notification.getIun(), recIndex, DocumentCreationTypeInt.AAR, timelineElementInternal.getElementId());
            } else
                log.debug("No need to recreate AAR iun={} timelineId={}", notification.getIun(), elementId);
        } catch (Exception e) {
            log.error("Cannot generate AAR pdf iun={} recIndex={} ex=", notification.getIun(), recIndex, e);
            throw new PnInternalException("cannot generate AAR pdf", ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED, e);
        }
    }

    public boolean addAarGenerationToTimeline(NotificationInt notification, Integer recIndex, PdfInfo pdfInfo) {
        return timelineService.addTimelineElement(
                timelineUtils.buildAarGenerationTimelineElement(notification, recIndex, pdfInfo.getKey(), pdfInfo.getNumberOfPages()),
                notification
        );
    }

    public AarGenerationDetailsInt getAarGenerationDetails(NotificationInt notification, Integer recIndex) {
        // ricostruisco il timelineid della  genrazione dell'aar
        String aarGenerationEventId = TimelineEventId.AAR_GENERATION.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );

        Optional<AarGenerationDetailsInt> detailOpt =
                timelineService.getTimelineElementDetails(notification.getIun(), aarGenerationEventId, AarGenerationDetailsInt.class);

        if (detailOpt.isEmpty() || !StringUtils.hasText(detailOpt.get().getGeneratedAarUrl()) || detailOpt.get().getNumberOfPages() == null) {
            log.error("Cannot retreieve AAR pdf safestoragekey iun={} detail={}", notification.getIun(), detailOpt);
            throw new PnInternalException("cannot retreieve AAR pdf safestoragekey", ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED);
        }
        return detailOpt.get();
    }

    public AarCreationRequestDetailsInt getAarCreationRequestDetailsInt(NotificationInt notification, Integer recIndex) {
        String elementId = timelineUtils.createEventIdForAarCreationRequest(notification.getIun(), recIndex);

        return timelineService.getTimelineElementDetails(notification.getIun(), elementId, AarCreationRequestDetailsInt.class)
                .orElseThrow(() -> new PnInternalException("Cannot retrieve AarCreationRequestDetails with elementId " + elementId, ERROR_CODE_DELIVERYPUSH_NOTFOUND));
    }

    public static boolean needDynamicAarRADDDefinition(String aarTemplateType) {
        String dynamicPropertyValue = org.apache.commons.lang3.StringUtils.substringBetween(aarTemplateType, START_DYNAMIC_PROPERTY_CHARACTER, END_DYNAMIC_PROPERTY_CHARACTER);
        return RADD_DYNAMIC_TEMPLATE_VALUE.equals(dynamicPropertyValue);
    }
}
