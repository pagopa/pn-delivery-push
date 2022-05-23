package it.pagopa.pn.deliverypush.middleware.timelinedao;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationRequestAccepted;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendDigitalDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.TimelineDaoDynamo;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.DtoToEntityTimelineMapper;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.EntityToDtoTimelineMapper;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

class TimelineDaoDynamoTest {

    private TimelineDao dao;
    private TimelineEntityDao entityDao;

    @BeforeEach
    void setup() {
        ObjectMapper objMapper = new ObjectMapper();

        DtoToEntityTimelineMapper dto2Entity = new DtoToEntityTimelineMapper(objMapper);
        EntityToDtoTimelineMapper entity2dto = new EntityToDtoTimelineMapper(objMapper);
        entityDao = new TestMyTimelineEntityDao();

        dao = new TimelineDaoDynamo(entityDao, dto2Entity, entity2dto);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void successfullyInsertAndRetrieve() {
        // GIVEN
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";

        String id1 = "sender_ack";
        TimelineElementInternal row1 = TimelineElementInternal.timelineInternalBuilder()
                .iun(iun)
                .elementId(id1)
                .category(TimelineElementCategory.REQUEST_ACCEPTED)
                .details(SmartMapper.mapToClass(new NotificationRequestAccepted(), TimelineElementDetails.class))
                .timestamp(Instant.now())
                .build();
        String id2 = "SendDigitalDetails";
        TimelineElementInternal row2 = TimelineElementInternal.timelineInternalBuilder()
                .iun(iun)
                .elementId(id2)
                .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE)
                .details(SmartMapper.mapToClass(new SendDigitalDetails(), TimelineElementDetails.class))
                .timestamp(Instant.now())
                .build();
        
        // WHEN
        dao.addTimelineElement(row1);
        dao.addTimelineElement(row2);

        // THEN
        // check first row
        Optional<TimelineElementInternal> retrievedRow1 = dao.getTimelineElement(iun, id1);
        Assertions.assertTrue(retrievedRow1.isPresent());
        Assertions.assertEquals(row1, retrievedRow1.get());

        // check second row
        Optional<TimelineElementInternal> retrievedRow2 = dao.getTimelineElement(iun, id2);
        Assertions.assertTrue(retrievedRow2.isPresent());
        Assertions.assertEquals(row2, retrievedRow2.get());

        // check full retrieve
        Set<TimelineElementInternal> result = dao.getTimeline(iun);
        Assertions.assertEquals(Set.of(row1, row2), result);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void successfullyDelete() {
        // GIVEN
        String iun = "iun1";

        String id1 = "sender_ack";
        TimelineElementInternal row1 = TimelineElementInternal.timelineInternalBuilder()
                .iun(iun)
                .elementId(id1)
                .category(TimelineElementCategory.REQUEST_ACCEPTED)
                .details(new TimelineElementDetails())
                .timestamp(Instant.now())
                .build();
        String id2 = "SendDigitalDetails";
        TimelineElementInternal row2 = TimelineElementInternal.timelineInternalBuilder()
                .iun(iun)
                .elementId(id2)
                .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE)
                .details(SmartMapper.mapToClass(new SendDigitalDetails(), TimelineElementDetails.class))
                .timestamp(Instant.now())
                .build();

        // WHEN
        dao.addTimelineElement(row1);
        dao.addTimelineElement(row2);

        dao.deleteTimeline(iun);

        // THEN
        Assertions.assertTrue(dao.getTimeline(iun).isEmpty());
    }

    private static class TestMyTimelineEntityDao implements TimelineEntityDao {

        private final Map<Key, TimelineElementEntity> store = new ConcurrentHashMap<>();

        @Override
        public void put(TimelineElementEntity timelineElementEntity) {
            Key key = Key.builder()
                    .partitionValue(timelineElementEntity.getIun())
                    .sortValue(timelineElementEntity.getTimelineElementId())
                    .build();
            this.store.put(key, timelineElementEntity);
        }

        @Override
        public void putIfAbsent(TimelineElementEntity timelineElementEntity) throws IdConflictException {
            Key key = Key.builder()
                    .partitionValue(timelineElementEntity.getIun())
                    .sortValue(timelineElementEntity.getTimelineElementId())
                    .build();

            if (this.store.put(key, timelineElementEntity) != null) {
                throw new IdConflictException(key);
            }
        }

        @Override
        public Optional<TimelineElementEntity> get(Key key) {
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public void delete(Key key) {
            store.remove(key);
        }


        @Override
        public Set<TimelineElementEntity> findByIun(String iun) {
            return this.store.values().stream()
                    .filter(el -> iun.equals(el.getIun()))
                    .collect(Collectors.toSet());
        }

        @Override
        public void deleteByIun(String iun) {
            Set<Key> toRemove = this.store.keySet().stream()
                    .filter(key -> iun.equals(key.partitionKeyValue().s()))
                    .collect(Collectors.toSet());

            for (Key key : toRemove) {
                this.store.remove(key);
            }
        }

    }

}