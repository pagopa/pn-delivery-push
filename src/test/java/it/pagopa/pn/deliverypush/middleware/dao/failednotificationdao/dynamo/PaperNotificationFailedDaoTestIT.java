package it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.dynamo;

import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@Import(LocalStackTestConfig.class)
@Disabled //per problema timeout su codebuild, ma in locale funziona
class PaperNotificationFailedDaoTestIT {

    @Autowired
    private PaperNotificationFailedDao paperNotificationFailedDao;


    @Test
    void deleteNotificationFailedWithEmptyResult() {
        String recipientId = "c-recipient";
        String iun = "iun-not-exists";
        var entities = paperNotificationFailedDao.getPaperNotificationFailedByRecipientId(recipientId);
        assertThat(entities).isEmpty();

        assertDoesNotThrow(() -> paperNotificationFailedDao.deleteNotificationFailed(recipientId, iun));
    }


}