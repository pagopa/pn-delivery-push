package it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.dynamo;

import it.pagopa.pn.deliverypush.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.dynamo.entity.PaperNotificationFailedEntity;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.dynamo.mapper.DtoToEntityNotificationFailedMapper;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.dynamo.mapper.EntityToDtoNotificationFailedMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Collections;
import java.util.Set;

class PaperNotificationFailedDaoDynamoTest {

    @Mock
    private PaperNotificationFailedEntityDao dao;

    @Mock
    private DtoToEntityNotificationFailedMapper dtoToEntity;

    @Mock
    private EntityToDtoNotificationFailedMapper entityToDto;

    private PaperNotificationFailedDaoDynamo dynamo;

    @BeforeEach
    void setUp() {
        dao = Mockito.mock(PaperNotificationFailedEntityDao.class);
        dtoToEntity = Mockito.mock(DtoToEntityNotificationFailedMapper.class);
        entityToDto = Mockito.mock(EntityToDtoNotificationFailedMapper.class);
        dynamo = new PaperNotificationFailedDaoDynamo(dao, dtoToEntity, entityToDto);
    }

    @Test
    void addPaperNotificationFailed() {
        PaperNotificationFailed dto = buildPaperNotificationFailed();
        PaperNotificationFailedEntity entity = buildPaperNotificationFailedEntity();

        Mockito.when(dtoToEntity.dto2Entity(dto)).thenReturn(entity);

        dynamo.addPaperNotificationFailed(dto);

        Mockito.verify(dao, Mockito.times(1)).put(entity);
    }

    @Test
    void getPaperNotificationFailedByRecipientId() {
        PaperNotificationFailed dto = buildPaperNotificationFailed();
        PaperNotificationFailedEntity entity = buildPaperNotificationFailedEntity();

        Mockito.when(entityToDto.entityToDto(entity)).thenReturn(dto);
        Mockito.when(dao.findByRecipientId("001")).thenReturn(Collections.singleton(entity));

        Set<PaperNotificationFailed> actual = dynamo.getPaperNotificationFailedByRecipientId("001");

        Assertions.assertEquals(1, actual.size());

    }

    @Test
    void deleteNotificationFailed() {
        Key key = Key.builder()
                .partitionValue("001")
                .sortValue("002")
                .build();

        dynamo.deleteNotificationFailed("001", "002");

        Mockito.verify(dao, Mockito.times(1)).delete(key);
    }

    private PaperNotificationFailed buildPaperNotificationFailed() {
        return PaperNotificationFailed.builder()
                .recipientId("001")
                .iun("002")
                .build();
    }

    private PaperNotificationFailedEntity buildPaperNotificationFailedEntity() {
        return PaperNotificationFailedEntity.builder()
                .recipientId("001")
                .iun("002")
                .build();
    }
}