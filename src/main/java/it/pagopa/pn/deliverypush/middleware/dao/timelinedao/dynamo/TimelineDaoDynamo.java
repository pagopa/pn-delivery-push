package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.DigitalAddressEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.PhysicalAddressEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementDetailsEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.DtoToEntityTimelineMapper;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.EntityToDtoTimelineMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = TimelineDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
@Slf4j
public class TimelineDaoDynamo implements TimelineDao {
    private final TimelineEntityDao entityDao;
    private final DtoToEntityTimelineMapper dto2entity;
    private final EntityToDtoTimelineMapper entity2dto;

    public TimelineDaoDynamo(TimelineEntityDao entityDao, DtoToEntityTimelineMapper dto2entity,
                             EntityToDtoTimelineMapper entity2dto) {
        this.entityDao = entityDao;
        this.dto2entity = dto2entity;
        this.entity2dto = entity2dto;
    }

    @Override
    public void addTimelineElementIfAbsent(TimelineElementInternal dto) throws PnIdConflictException {
        TimelineElementEntity entity = getTimelineElementEntity(dto);

        entityDao.putIfAbsent(entity);
    }
    
    @NotNull
    private TimelineElementEntity getTimelineElementEntity(TimelineElementInternal dto) {
        TimelineElementEntity entity = dto2entity.dtoToEntity(dto);

        TimelineElementDetailsEntity details = entity.getDetails();
        if (details != null) {

            TimelineElementDetailsEntity newDetails = cloneWithoutSensitiveInformation(details);
            entity.setDetails(newDetails);
        }
        return entity;
    }
    
    @NotNull
    private TimelineElementDetailsEntity cloneWithoutSensitiveInformation(TimelineElementDetailsEntity details) {
        TimelineElementDetailsEntity newDetails = details.toBuilder().build();
        
        PhysicalAddressEntity physicalAddress = newDetails.getPhysicalAddress();
        if( physicalAddress != null ) {
            newDetails.setPhysicalAddress( physicalAddress.toBuilder()
                            .at(null)
                            .municipalityDetails(null)
                            .zip(null)
                            .foreignState(null)
                            .addressDetails(null)
                            .province(null)
                            .municipality(null)
                            .address(null)
                    .build());
        }
        
        PhysicalAddressEntity newAddress = newDetails.getNewAddress();
        if( newAddress != null ) {
            newDetails.setNewAddress( newAddress.toBuilder()
                    .at(null)
                    .municipalityDetails(null)
                    .zip(null)
                    .addressDetails(null)
                    .province(null)
                    .municipality(null)
                    .foreignState(null)
                    .address(null)
                    .build());
        }

        DigitalAddressEntity digitalAddress = newDetails.getDigitalAddress();
        if( digitalAddress != null ) {
            newDetails.setDigitalAddress( digitalAddress.toBuilder()
                    .address( null )
                    .build());
        }

        
        return newDetails;
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        Key keyToSearch = Key.builder()
                .partitionValue(iun)
                .sortValue(timelineId)
                .build();
        return entityDao.get(keyToSearch)
                .map(entity2dto::entityToDto);
    }

    @Override
    public Set<TimelineElementInternal> getTimeline(String iun) {
        return entityDao.findByIun(iun)
                .stream()
                .map(entity2dto::entityToDto)
                .collect(Collectors.toSet());
    }


    @Override
    public Set<TimelineElementInternal> getTimelineFilteredByElementId(String iun, String elementId) {
        return entityDao.searchByIunAndElementId(iun, elementId)
                .stream()
                .map(entity2dto::entityToDto)
                .collect(Collectors.toSet());
    }



    @Override
    public void deleteTimeline(String iun) {
        entityDao.deleteByIun(iun);
    }

}
