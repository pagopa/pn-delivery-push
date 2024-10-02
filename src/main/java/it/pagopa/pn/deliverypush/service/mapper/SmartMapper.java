package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV23;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;


@Slf4j
public class SmartMapper {
    private static ModelMapper modelMapper;


    private static BiFunction postMappingTransformer;

    private SmartMapper (){}

    private static String SERCQ_SEND = "send-self";


    static PropertyMap<NormalizedAddressDetailsInt, TimelineElementDetailsV23> addressDetailPropertyMap = new PropertyMap<>() {
        @Override
        protected void configure() {
            skip(destination.getNewAddress());
            skip(destination.getPhysicalAddress());
        }
    };


    static PropertyMap<PrepareAnalogDomicileFailureDetailsInt, TimelineElementDetailsV23> prepareAnalogDomicileFailureDetailsInt = new PropertyMap<>() {
        @Override
        protected void configure() {
            skip(destination.getPhysicalAddress());
        }
    };
    static Converter<TimelineElementInternal, TimelineElementInternal> timelineElementInternalTimestampConverter =
            ctx -> {
                // se il detail estende l'interfaccia e l'elementTimestamp non è nullo, lo sovrascrivo nel source originale
                if (ctx.getSource().getDetails() instanceof ElementTimestampTimelineElementDetails elementTimestampTimelineElementDetails
                    && elementTimestampTimelineElementDetails.getElementTimestamp() != null)
                {
                    return ctx.getSource().toBuilder()
                            .timestamp(elementTimestampTimelineElementDetails.getElementTimestamp())
                            .build();
                }

                return ctx.getSource();
            };

    static{
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.addMappings(addressDetailPropertyMap);
        modelMapper.addMappings(prepareAnalogDomicileFailureDetailsInt);

        modelMapper.createTypeMap(TimelineElementInternal.class, TimelineElementInternal.class).setPostConverter(timelineElementInternalTimestampConverter);

        List<BiFunction> postMappingTransformers = new ArrayList<>();
        postMappingTransformers.add( (source, result)-> {
            if (!(source instanceof NotificationCancelledDetailsInt) && result instanceof TimelineElementDetailsV23){
                ((TimelineElementDetailsV23) result).setNotRefinedRecipientIndexes(null);
            }
            return result;
        });

        postMappingTransformer =  postMappingTransformers.stream()
            .reduce((f, g) -> (i, s) -> f.apply(i, g.apply(i, s)))
            .get();
    }

    /*
        Mapping effettuato per la modifica dei timestamp per gli
        elementi di timeline che implementano l'interfaccia ElementTimestampTimelineElementDetails
     */
    public static  <S,T> T mapToClass(S source, Class<T> destinationClass ){
        T result;
        if( source != null) {
            result = modelMapper.map(source, destinationClass );

            result = (T) postMappingTransformer.apply(source, result);
        } else {
            result = null;
        }
        return result;
    }

    /*
        Mapping effettuato per la modifica dei timestamp per gli
        elementi di timeline che implementano l'interfaccia ElementTimestampTimelineElementDetails
     */
    private static  TimelineElementInternal mapTimelineInternal(TimelineElementInternal source ){
        TimelineElementInternal result;
        if( source != null) {
            TimelineElementInternal elementToMap = source.toBuilder().build();
            result = modelMapper.map(elementToMap, TimelineElementInternal.class );
            result = (TimelineElementInternal) postMappingTransformer.apply(source, result);
        } else {
            result = null;
        }
        return result;
    }

    /**
        Remapping per gli elementi di timeline per il workflow analogico (workaround per PN-9059)
        I restanti remapping vengono gestiti tramite il typeMap e l'interfaccia ElementTimestampTimelineElementDetails
     */
    public static TimelineElementInternal mapTimelineInternal(TimelineElementInternal source, Set<TimelineElementInternal> timelineElementInternalSet) {
        //Viene recuperato il timestamp originale, prima di effettuare un qualsiasi remapping
        Instant ingestionTimestamp = source.getTimestamp();

        //Viene effettuato un primo remapping degli elementi di timeline e dei relativi timestamp in particolare viene effettuato il remapping di tutti 
        // i timestamp che non dipendono da ulteriori elementi di timeline, cioè hanno l'eventTimestamp già storicizzato nei details
        TimelineElementInternal result = mapTimelineInternal(source);

        if(result != null) {
            //L'ingestion timestamp viene settato con il timestamp originale dell'evento (dunque timestamp evento per SEND)
            result.setIngestionTimestamp(ingestionTimestamp);

            //Nello switch case invece vengono effettuati ulteriori remapping dei timestamp, questi non dipendono dal singolo elemento, ma necessitano di tutta la timeline
            switch (result.getCategory()) {
                case SCHEDULE_REFINEMENT -> {
                    Instant endAnalogWorkflowBusinessDate =  computeEndAnalogWorkflowBusinessData((RecipientRelatedTimelineElementDetails)result.getDetails(), timelineElementInternalSet, result.getIun());
                    if(endAnalogWorkflowBusinessDate != null){
                        log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {} ", result.getCategory(), result.getTimestamp(), endAnalogWorkflowBusinessDate);
                        result.setTimestamp(endAnalogWorkflowBusinessDate);
                    }
                }
                case ANALOG_SUCCESS_WORKFLOW, ANALOG_FAILURE_WORKFLOW, COMPLETELY_UNREACHABLE_CREATION_REQUEST, COMPLETELY_UNREACHABLE -> {
                    Instant endAnalogWorkflowBusinessDate = computeEndAnalogWorkflowBusinessData((RecipientRelatedTimelineElementDetails)result.getDetails(), timelineElementInternalSet, result.getIun());
                    if(endAnalogWorkflowBusinessDate != null){
                        log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {} ", result.getCategory(), result.getTimestamp(), endAnalogWorkflowBusinessDate);
                        result.setTimestamp(endAnalogWorkflowBusinessDate);
                    }else{
                        log.error("SEARCH LAST SEND_ANALOG_FEEDBACK DETAILS NULL element {}",result);
                        throw new PnInternalException("SEND_ANALOG_FEEDBACK NOT PRESENT, ERROR IN MAPPING", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
                    }
                }
                case REFINEMENT -> {
                    ScheduleRefinementDetailsInt details = findScheduleRefinementDetails((RecipientRelatedTimelineElementDetails)result.getDetails(),timelineElementInternalSet);
                    if(details != null){
                        log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {}", result.getCategory(), result.getTimestamp(), details.getSchedulingDate());
                        result.setTimestamp(details.getSchedulingDate());
                    }else{
                        throw new PnInternalException("SCHEDULE_REFINEMENT NOT PRESENT, ERROR IN MAPPING", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
                    }
                }
                case SEND_DIGITAL_DOMICILE -> {
                    // allo scopo di ottenere il timestamp di AAR_GEN per impostare il timestamp di SEND_DIGITAL_DOMICILE, per far si
                    // che il timestamp di SEND_DIGITAL_DOMICILE non sia successivo a quello di SEND_DIGITAL_FEEDBACK per serc SEND
                    //
                    // ottenere channelType e verificare che è sercq
                    // se sercq, ottenere l'address, e se è sercq-send:
                    //  - ottieniamo timestamp di AAR_GEN
                    // - lo usiamo per impostare il timestamp di SEND_DIGITAL_DOMICILE (setTimeStamp)
                    //
                    SendDigitalDetailsInt details = (SendDigitalDetailsInt) result.getDetails();
                    if (LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ.equals(details.getDigitalAddress().getType()) &&
                            details.getDigitalAddress().getAddress().contains(SERCQ_SEND)) {
                            Instant aarRgenTimestamp = findAARgenTimestamp((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet);
                            result.setTimestamp(aarRgenTimestamp);
                    }
                }
                default -> {
                    //nothing to do
                }
            }
            
            //In ultima istanza viene settato l'eventTimestamp con il timestamp rimappato (avranno dunque in uscita sempre lo stesso valore)
            result.setEventTimestamp(result.getTimestamp());
        }

        return result;
    }

    private static Instant findAARgenTimestamp(RecipientRelatedTimelineElementDetails elementDetails, Set<TimelineElementInternal> timelineElementInternalSet) {
        if(elementDetails == null){
            throw new PnInternalException("ELEMENT DETAILS NULL", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
        int recIndex = elementDetails.getRecIndex();

        TimelineElementInternal aarGenerationTimelineElement = timelineElementInternalSet.stream().filter(e ->
                e.getCategory() == TimelineElementCategoryInt.AAR_GENERATION &&
                        e.getDetails() instanceof RecipientRelatedTimelineElementDetails aarGenerationTimelineElementDetails &&
                        aarGenerationTimelineElementDetails.getRecIndex() == recIndex
        ).findFirst().orElseThrow(() -> new PnInternalException("SCHEDULE_REFINEMENT NOT PRESENT, ERROR IN MAPPING", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT));

        return aarGenerationTimelineElement.getTimestamp();
    }


    private static ScheduleRefinementDetailsInt findScheduleRefinementDetails(RecipientRelatedTimelineElementDetails elementDetails, Set<TimelineElementInternal> timelineElementInternalSet) {
        if(elementDetails == null){
            throw new PnInternalException("ELEMENT DETAILS NULL", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
        int recIndex = elementDetails.getRecIndex();

        TimelineElementInternal scheduleRefinementTimelineElment = timelineElementInternalSet.stream().filter(e ->
                e.getCategory() == TimelineElementCategoryInt.SCHEDULE_REFINEMENT &&
                        e.getDetails() instanceof RecipientRelatedTimelineElementDetails scheduleRefinementTimelineElementDetails &&
                        scheduleRefinementTimelineElementDetails.getRecIndex() == recIndex
        ).findFirst().orElseThrow(() -> new PnInternalException("SCHEDULE_REFINEMENT NOT PRESENT, ERROR IN MAPPING", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT));

        return (ScheduleRefinementDetailsInt) scheduleRefinementTimelineElment.getDetails();
    }

    private static Instant computeEndAnalogWorkflowBusinessData(RecipientRelatedTimelineElementDetails elementDetails, Set<TimelineElementInternal> timelineElementInternalSet, String iun) {
        if(elementDetails == null){
            throw new PnInternalException("ELEMENT DETAILS NULL", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
        int recIndex = elementDetails.getRecIndex();

        Instant endAnalogWorkflowBusinessDate = timelineElementInternalSet.stream().filter(elem ->
                isElementAffectingEndAnalogWorkflowBusinessData(elem,recIndex)
        ).map(SmartMapper::extractBusinessDate).max(Instant::compareTo).orElse(null);

        log.debug("Business end analog workflow for iun {} is {} ", iun, endAnalogWorkflowBusinessDate);
        return endAnalogWorkflowBusinessDate;
    }

    private static Instant extractBusinessDate(TimelineElementInternal timelineElementInternal){
        if(timelineElementInternal.getDetails() instanceof SendAnalogFeedbackDetailsInt sendAnalogFeedbackDetails){
            return sendAnalogFeedbackDetails.getNotificationDate();
        }else if(timelineElementInternal.getDetails() instanceof PrepareAnalogDomicileFailureDetailsInt){
            return timelineElementInternal.getTimestamp();
        }else{
            throw new PnInternalException("Illegal state: iun "+timelineElementInternal.getIun(), PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR);
        }
    }

    private static boolean isElementAffectingEndAnalogWorkflowBusinessData(TimelineElementInternal elementInternal, Integer recIndex){
        boolean isValidCategory = (elementInternal.getCategory() == TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK ||
                elementInternal.getCategory() == TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE_FAILURE);

        boolean isValidRecIndex = false;

        if(isValidCategory && elementInternal.getDetails() instanceof RecipientRelatedTimelineElementDetails details){
            isValidRecIndex = details.getRecIndex() == recIndex;
        }

        return isValidRecIndex;
    }

}
