package it.pagopa.pn.deliverypush.middleware.failednotificationdao;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        TimelineDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        PaperNotificationFailedDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        "aws.region-code=us-east-1",
        "aws.profile-name=default",
        "aws.endpoint-url=http://localhost:4566",
        "aws.region-code=us-east-1",
        "aws.profile-name=default",
        "aws.endpoint-url=http://localhost:4566",
})
@SpringBootTest
class PaperNotificationFailedDaoDynamoTestIT {

    @Autowired
    private PaperNotificationFailedDao specificDao;
    @Autowired
    private PaperNotificationFailedEntityDao daoEntity;

    @Test
    void addPaperNotificationOk() {
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d38";
        String idRecipient = "paMi3";

        deletePaperNotificationFailed(iun, idRecipient);

        PaperNotificationFailed failedNot = PaperNotificationFailed.builder()
                .iun(iun)
                .recipientId(idRecipient).build();
        specificDao.addPaperNotificationFailed(failedNot);

        Set<PaperNotificationFailed> res = specificDao.getNotificationByRecipientId(idRecipient)
                .stream()
                .filter(pnf -> iun.equals(pnf.getIun()))
                .filter(pnf -> idRecipient.equals(pnf.getRecipientId()))
                .collect(Collectors.toSet());

        assertEquals(1, res.size());
    }

    @Test
    void deleteNotificationFailedOk() {
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d41";
        String idRecipient = "paMi4";

        PaperNotificationFailed failedNot = PaperNotificationFailed.builder()
                .iun(iun)
                .recipientId(idRecipient).build();

        specificDao.addPaperNotificationFailed(failedNot);

        deletePaperNotificationFailed(iun, idRecipient);

        Set<PaperNotificationFailed> res = specificDao.getNotificationByRecipientId(idRecipient)
                .stream()
                .filter(pnf -> iun.equals(pnf.getIun()))
                .filter(pnf -> idRecipient.equals(pnf.getRecipientId()))
                .collect(Collectors.toSet());

        assertEquals(0, res.size());
    }

    private void deletePaperNotificationFailed(String iun, String idRecipient) {
        specificDao.deleteNotificationFailed(idRecipient, iun);
    }
    
    @Test
    void getNotificationByRecipientId() {
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d41";
        String idRecipient = "paMi4";
        
        deletePaperNotificationFailed(iun, idRecipient);

        PaperNotificationFailed failedNot = PaperNotificationFailed.builder()
                .iun(iun)
                .recipientId(idRecipient).build();

        specificDao.addPaperNotificationFailed(failedNot);
        
        Set<PaperNotificationFailed> res = specificDao.getNotificationByRecipientId(idRecipient)
                .stream()
                .filter(pnf -> iun.equals(pnf.getIun()))
                .filter(pnf -> idRecipient.equals(pnf.getRecipientId()))
                .collect(Collectors.toSet());

        assertEquals(1, res.size());
        assertTrue(res.contains(failedNot));

    }
}