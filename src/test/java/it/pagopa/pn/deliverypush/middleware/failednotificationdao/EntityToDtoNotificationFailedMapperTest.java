package it.pagopa.pn.deliverypush.middleware.failednotificationdao;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.dynamo.mapper.EntityToDtoNotificationFailedMapper;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.dynamo.entity.PaperNotificationFailedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityToDtoNotificationFailedMapperTest {

    private EntityToDtoNotificationFailedMapper entityToDtoNotificationFailedMapper;

    @BeforeEach
    void instantiateDao() {
        entityToDtoNotificationFailedMapper = new EntityToDtoNotificationFailedMapper();
    }

    @Test
    void entityToDto() {
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";
        String idRecipient = "paMi2";
        PaperNotificationFailedEntity ent = PaperNotificationFailedEntity.builder()
                .recipientId(idRecipient)
                .iun(iun)
                .build();

        PaperNotificationFailed dto = entityToDtoNotificationFailedMapper.entityToDto(ent);

        assertEquals(dto.getIun(), ent.getIun());
        assertEquals(dto.getRecipientId(), ent.getRecipientId());
    }
}