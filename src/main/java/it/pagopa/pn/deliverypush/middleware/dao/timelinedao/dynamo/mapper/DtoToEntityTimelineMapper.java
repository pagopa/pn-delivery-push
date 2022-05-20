package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.LegalFactsIdEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementCategoryEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementDetailsEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DtoToEntityTimelineMapper {
    private final ObjectMapper objectMapper;

    public DtoToEntityTimelineMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    public TimelineElementEntity dtoToEntity(TimelineElementInternal dto) {
        return TimelineElementEntity.builder()
                .iun( dto.getIun() )
                .timelineElementId( dto.getElementId() )
                .category( TimelineElementCategoryEntity.valueOf(dto.getCategory().getValue()) )
                .timestamp( dto.getTimestamp() )
                .details( dtoToDetailsEntity( dto.getDetails() ) )
                .legalFactId( legalFactsDtoToString( dto.getLegalFactsIds() ) )
                .build();
    }

    private String legalFactsDtoToString(List<LegalFactsId> legalFactsIds) {
        String legalFacts = null;
                
        if(legalFactsIds != null){
            List<LegalFactsIdEntity> listLegalFactsEntity =  legalFactsIds.stream().map(
                    legalFactsId -> SmartMapper.mapToClass(legalFactsId, LegalFactsIdEntity.class )
            ).collect(Collectors.toList());
            legalFacts = legalFactIdsToJsonString(listLegalFactsEntity);
        }
        
        return legalFacts;
    }

    private String legalFactIdsToJsonString(List<LegalFactsIdEntity> listLegalFactsEntity) {
        try {
            return objectMapper.writeValueAsString( listLegalFactsEntity );
        } catch (JsonProcessingException exc) {
            throw new PnInternalException( "Writing timeline detail to storage", exc );
        }
    }

    private TimelineElementDetailsEntity dtoToDetailsEntity(TimelineElementDetails details) {
        return SmartMapper.mapToClass(details, TimelineElementDetailsEntity.class );

        /*
        TimelineElementDetailsEntity.TimelineElementDetailsEntityBuilder detailsEntityBuilder = TimelineElementDetailsEntity.builder()
                .recIndex(details.getRecIndex())
                .digitalAddressSource(details.getDigitalAddressSource() != null ? DigitalAddressSourceEntity.valueOf(details.getDigitalAddressSource().getValue()) : null )
                .isAvailable(details.getIsAvailable())
                .attemptDate(details.getAttemptDate())
                .deliveryMode(details.getDeliveryMode() != null ? DeliveryModeEntity.valueOf(details.getDeliveryMode().getValue()) : null )
                .contactPhase(details.getContactPhase() != null ? ContactPhaseEntity.valueOf(details.getContactPhase().getValue()) : null)
                .sentAttemptMade(details.getSentAttemptMade())
                .sendDate(details.getSendDate())
                .errors(details.getErrors())
                .lastAttemptDate(details.getLastAttemptDate())
                .retryNumber(details.getRetryNumber())
                .responseStatus(details.getResponseStatus() != null ? ResponseStatusEntity.valueOf(details.getResponseStatus().getValue()) : null)
                .notificationDate(details.getNotificationDate())
                .serviceLevel(details.getServiceLevel() != null ? ServiceLevel.valueOf(details.getServiceLevel().getValue()) : null)
                .investigation(details.getInvestigation());

        PhysicalAddress physicalAddress = details.getPhysicalAddress();
        if(physicalAddress != null){
            detailsEntityBuilder.physicalAddress(
                    PhysicalAddressEntity.builder()
                            .at(physicalAddress.getAt())
                            .address(physicalAddress.getAddress())
                            .addressDetails(physicalAddress.getAddressDetails())
                            .foreignState(physicalAddress.getForeignState())
                            .municipality(physicalAddress.getMunicipality())
                            .municipalityDetails(physicalAddress.getMunicipalityDetails())
                            .province(physicalAddress.getProvince())
                            .zip(physicalAddress.getZip())
                            .build()
            );
        }

        PhysicalAddress newAddress = details.getNewAddress();
        if(newAddress != null){
            detailsEntityBuilder.physicalAddress(
                    PhysicalAddressEntity.builder()
                            .at(newAddress.getAt())
                            .address(newAddress.getAddress())
                            .addressDetails(newAddress.getAddressDetails())
                            .foreignState(newAddress.getForeignState())
                            .municipality(newAddress.getMunicipality())
                            .municipalityDetails(newAddress.getMunicipalityDetails())
                            .province(newAddress.getProvince())
                            .zip(newAddress.getZip())
                            .build()
            );
        }

        DigitalAddress digitalAddress = details.getDigitalAddress();
        if(digitalAddress != null){
            detailsEntityBuilder.digitalAddress(
                    DigitalAddressEntity.builder()
                            .address(digitalAddress.getAddress())
                            .type(digitalAddress.getType() != null ? DigitalAddressEntity.TypeEnum.valueOf(digitalAddress.getType().getValue()) : null)
                            .build()
            );
        }

        DownstreamId downstreamId = details.getDownstreamId();
        if(downstreamId != null){
            detailsEntityBuilder.downstreamId(
                    DownstreamIdEntity.builder()
                            .messageId(downstreamId.getMessageId())
                            .systemId(downstreamId.getSystemId())
                            .build()
            );
        }
        
        return detailsEntityBuilder.build();
        
         */
    }
}
