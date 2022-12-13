package it.pagopa.pn.deliverypush.middleware.timelinedao;

import io.swagger.models.auth.In;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationRequestAcceptedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.TimelineDaoDynamo;
import it.pagopa.pn.deliverypush.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.DtoToEntityTimelineMapper;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.EntityToDtoTimelineMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

class TimelineDaoDynamoTest {

    private TimelineDao dao;

    @BeforeEach
    void setup() {

        DtoToEntityTimelineMapper dto2Entity = new DtoToEntityTimelineMapper();
        EntityToDtoTimelineMapper entity2dto = new EntityToDtoTimelineMapper();
        TimelineEntityDao entityDao = new TestMyTimelineEntityDao();

        dao = new TimelineDaoDynamo(entityDao, dto2Entity, entity2dto);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void successfullyInsertAndRetrieve() {
        // GIVEN
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";

        String id1 = "sender_ack";
        TimelineElementInternal row1 = TimelineElementInternal.builder()
                .iun(iun)
                .elementId(id1)
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details( NotificationRequestAcceptedDetailsInt.builder().build() )
                .timestamp(Instant.now())
                .statusInfo(StatusInfoInternal.builder().build())
                .build();
        String id2 = "SendDigitalDetails";
        TimelineElementInternal row2 = TimelineElementInternal.builder()
                .iun(iun)
                .elementId(id2)
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .details( SendDigitalDetailsInt.builder().build() )
                .timestamp(Instant.now())
                .statusInfo(StatusInfoInternal.builder().build())
                .build();
        
        // WHEN
        dao.addTimelineElementIfAbsent(row1);
        dao.addTimelineElementIfAbsent(row2);

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
    void successfullyInsertAndRetrieveSearch() {
        // GIVEN
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";
        String id_prefix = "SendDigitalDetails_";

        String id1 = "sender_ack";
        TimelineElementInternal row1 = TimelineElementInternal.builder()
                .iun(iun)
                .elementId(id1)
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details( NotificationRequestAcceptedDetailsInt.builder().build() )
                .timestamp(Instant.now())
                .statusInfo(StatusInfoInternal.builder().build())
                .notificationSentAt(Instant.now())
                .build();
        String id2 = id_prefix + "1";
        TimelineElementInternal row2 = TimelineElementInternal.builder()
                .iun(iun)
                .elementId(id2)
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .details( SendDigitalDetailsInt.builder().build() )
                .timestamp(Instant.now())
                .statusInfo(StatusInfoInternal.builder().build())
                .notificationSentAt(Instant.now())
                .build();
        String id3 = id_prefix + "2";
        TimelineElementInternal row3 = TimelineElementInternal.builder()
                .iun(iun)
                .elementId(id3)
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .details( SendDigitalDetailsInt.builder().build() )
                .timestamp(Instant.now())
                .statusInfo(StatusInfoInternal.builder().build())
                .notificationSentAt(Instant.now())
                .build();

        // WHEN
        dao.addTimelineElementIfAbsent(row1);
        dao.addTimelineElementIfAbsent(row2);
        dao.addTimelineElementIfAbsent(row3);

        // THEN


        // check full retrieve
        Set<TimelineElementInternal> result = dao.getTimelineFilteredByElementId(iun, id_prefix);
        Assertions.assertEquals(Set.of(row2, row3), result);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void successfullyDelete() {
        // GIVEN
        String iun = "iun1";

        StatusInfoInternal statusInfo = Mockito.mock(StatusInfoInternal.class);

        String id1 = "sender_ack";
        TimelineElementInternal row1 = TimelineElementInternal.builder()
                .iun(iun)
                .elementId(id1)
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details( NotificationRequestAcceptedDetailsInt.builder().build() )
                .timestamp(Instant.now())
                .statusInfo(statusInfo)
                .notificationSentAt(Instant.now())
                .build();
        String id2 = "SendDigitalDetails";
        TimelineElementInternal row2 = TimelineElementInternal.builder()
                .iun(iun)
                .elementId(id2)
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .details( SendDigitalDetailsInt.builder().build() )
                .timestamp(Instant.now())
                .statusInfo(statusInfo)
                .notificationSentAt(Instant.now())
                .build();

        // WHEN
        dao.addTimelineElementIfAbsent(row1);
        dao.addTimelineElementIfAbsent(row2);

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
        public void putIfAbsent(TimelineElementEntity timelineElementEntity) throws PnIdConflictException {
            Key key = Key.builder()
                    .partitionValue(timelineElementEntity.getIun())
                    .sortValue(timelineElementEntity.getTimelineElementId())
                    .build();

            if (this.store.put(key, timelineElementEntity) != null) {
                throw new PnIdConflictException(Collections.singletonMap("errorKey", key.toString()));
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
        public Set<TimelineElementEntity> searchByIunAndElementId(String iun, String elementId) {
            return this.store.values().stream()
                    .filter(el -> iun.equals(el.getIun()) && el.getTimelineElementId().startsWith(elementId))
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