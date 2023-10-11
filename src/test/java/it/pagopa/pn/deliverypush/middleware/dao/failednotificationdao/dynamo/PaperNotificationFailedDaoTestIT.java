package it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.dynamo;

import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import it.pagopa.pn.deliverypush.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@Import(LocalStackTestConfig.class)
class PaperNotificationFailedDaoTestIT {


    @Autowired
    private PaperNotificationFailedDao paperNotificationFailedDao;


    @Test
    void addPaperNotificationFailedTest() {
        String iun = UUID.randomUUID().toString();
        String recipientId = "a-recipientId";
        PaperNotificationFailed dto = buildPaperNotificationFailed(recipientId, iun);
        paperNotificationFailedDao.addPaperNotificationFailed(dto);

        var entities = paperNotificationFailedDao.getPaperNotificationFailedByRecipientId(dto.getRecipientId());
        assertThat(entities).hasSize(1);

        var paperNotificationFailedArray = new ArrayList<>(entities);
        assertThat(paperNotificationFailedArray.get(0).getIun()).isEqualTo(dto.getIun());
        assertThat(paperNotificationFailedArray.get(0).getRecipientId()).isEqualTo(dto.getRecipientId());

        paperNotificationFailedDao.deleteNotificationFailed(recipientId, iun);
        entities = paperNotificationFailedDao.getPaperNotificationFailedByRecipientId(dto.getRecipientId());
        assertThat(entities).isEmpty();
    }

    @Test
    void getPaperNotificationFailedByRecipientIdTest() {
        String iun = "a" + UUID.randomUUID();
        String recipientId = "b-recipientId";
        String anotherIun = "b" + UUID.randomUUID();
        PaperNotificationFailed dto = buildPaperNotificationFailed(recipientId, iun);
        PaperNotificationFailed dtoTwo = buildPaperNotificationFailed(recipientId, anotherIun);
        paperNotificationFailedDao.addPaperNotificationFailed(dto);
        paperNotificationFailedDao.addPaperNotificationFailed(dtoTwo);

        var entities = paperNotificationFailedDao.getPaperNotificationFailedByRecipientId(dto.getRecipientId());
        assertThat(entities).hasSize(2);

        var paperNotificationFailedArray = new ArrayList<>(entities);
        paperNotificationFailedArray.sort(Comparator.comparing(PaperNotificationFailed::getIun));
        assertThat(paperNotificationFailedArray.get(0).getIun()).isEqualTo(dto.getIun());
        assertThat(paperNotificationFailedArray.get(0).getRecipientId()).isEqualTo(dto.getRecipientId());
        assertThat(paperNotificationFailedArray.get(1).getIun()).isEqualTo(dtoTwo.getIun());
        assertThat(paperNotificationFailedArray.get(0).getRecipientId()).isEqualTo(dtoTwo.getRecipientId());

        paperNotificationFailedDao.deleteNotificationFailed(recipientId, iun);
        paperNotificationFailedDao.deleteNotificationFailed(recipientId, anotherIun);
        entities = paperNotificationFailedDao.getPaperNotificationFailedByRecipientId(dto.getRecipientId());
        assertThat(entities).isEmpty();

    }

    @Test
    void deleteNotificationFailedWithEmptyResult() {
        String recipientId = "c-recipient";
        String iun = "iun-not-exists";
        var entities = paperNotificationFailedDao.getPaperNotificationFailedByRecipientId(recipientId);
        assertThat(entities).isEmpty();

        assertDoesNotThrow(() -> paperNotificationFailedDao.deleteNotificationFailed(recipientId, iun));
    }

    private PaperNotificationFailed buildPaperNotificationFailed(String recipientId, String iun) {
        return PaperNotificationFailed.builder()
                .recipientId(recipientId)
                .iun(iun)
                .build();
    }

}