package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReworkUtilsTest {

    private NotificationReworksEntity buildEntity(String reworkId) {
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setReworkId(reworkId);
        return entity;
    }

    @Test
    void testGetLatestReworkRequestWithMultipleItems() {
        NotificationReworksEntity e1 = buildEntity("REWORK_1.TRY_2");
        NotificationReworksEntity e2 = buildEntity("REWORK_2.TRY_1");
        NotificationReworksEntity e3 = buildEntity("REWORK_2.TRY_3");
        NotificationReworksEntity e4 = buildEntity("REWORK_1.TRY_5");

        List<NotificationReworksEntity> entities = List.of(e1, e2, e3, e4);
        NotificationReworksEntity latest = ReworkUtils.getLatestReworkRequest(entities).block();

        assertNotNull(latest);
        assertEquals("REWORK_2.TRY_3", latest.getReworkId());
    }

    @Test
    void testGetLatestReworkRequestWithSameReworkDifferentTry() {
        NotificationReworksEntity e1 = buildEntity("REWORK_3.TRY_1");
        NotificationReworksEntity e2 = buildEntity("REWORK_3.TRY_5");
        NotificationReworksEntity e3 = buildEntity("REWORK_3.TRY_2");

        List<NotificationReworksEntity> entities = List.of(e1, e2, e3);
        NotificationReworksEntity latest = ReworkUtils.getLatestReworkRequest(entities).block();

        assertNotNull(latest);
        assertEquals("REWORK_3.TRY_5", latest.getReworkId());
    }

    @Test
    void testGetLatestReworkRequestWithDifferentReworkIDsAndSameTry() {
        NotificationReworksEntity e1 = buildEntity("REWORK_1.TRY_1");
        NotificationReworksEntity e2 = buildEntity("REWORK_2.TRY_1");

        List<NotificationReworksEntity> entities = List.of(e1, e2);
        NotificationReworksEntity latest = ReworkUtils.getLatestReworkRequest(entities).block();

        assertNotNull(latest);
        assertEquals("REWORK_2.TRY_1", latest.getReworkId());
    }

    @Test
    void testGetLatestReworkRequestWithDifferentReworkIDsAndTry() {
        NotificationReworksEntity e1 = buildEntity("REWORK_2.TRY_1");
        NotificationReworksEntity e2 = buildEntity("REWORK_1.TRY_2");

        List<NotificationReworksEntity> entities = List.of(e1, e2);
        NotificationReworksEntity latest = ReworkUtils.getLatestReworkRequest(entities).block();

        assertNotNull(latest);
        assertEquals("REWORK_2.TRY_1", latest.getReworkId());
    }

    @Test
    void testGetLatestReworkRequestWithSingleElement() {
        NotificationReworksEntity e1 = buildEntity("REWORK_1.TRY_1");
        List<NotificationReworksEntity> entities = List.of(e1);

        NotificationReworksEntity latest = ReworkUtils.getLatestReworkRequest(entities).block();

        assertNotNull(latest);
        assertEquals("REWORK_1.TRY_1", latest.getReworkId());
    }
}
