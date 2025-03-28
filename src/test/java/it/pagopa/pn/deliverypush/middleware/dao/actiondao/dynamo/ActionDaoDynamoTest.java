package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.io.IoSendMessageResultInt;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.FutureActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class ActionDaoDynamoTest {

    @Mock
    private ActionEntityDao actionEntityDao;

    @Mock
    private FutureActionEntityDao futureActionEntityDao;
    
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    @Mock
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Mock
    private ActionDaoDynamo dynamo;

    @BeforeEach
    void setup() {

        PnDeliveryPushConfigs.ActionDao actionDao = new PnDeliveryPushConfigs.ActionDao();
        actionDao.setTableName("Action");
        PnDeliveryPushConfigs.FutureActionDao factionDao = new PnDeliveryPushConfigs.FutureActionDao();
        factionDao.setTableName("FutureAction");
        Mockito.when(pnDeliveryPushConfigs.getActionDao()).thenReturn(actionDao);
        Mockito.when(pnDeliveryPushConfigs.getActionTtlDays()).thenReturn("1095");
        Mockito.when(pnDeliveryPushConfigs.getFutureActionDao()).thenReturn(factionDao);

        dynamo = new ActionDaoDynamo(actionEntityDao, futureActionEntityDao,
                dynamoDbEnhancedClient, pnDeliveryPushConfigs);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void addAction() {
        String timeslot = "2022-08-30T16:04:13.913859900Z";
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);
        ActionEntity actionEntity = buildActionEntity(action);
        FutureActionEntity futureActionEntity = buildFutureActionEntity(action, timeslot);
        
        dynamo.addAction(action, timeslot);

        ArgumentCaptor<ActionEntity> actionEntityArgumentCaptor = ArgumentCaptor.forClass(ActionEntity.class);

        Mockito.verify(actionEntityDao, Mockito.times(1)).put(actionEntityArgumentCaptor.capture());
        ActionEntity capturedEntity = actionEntityArgumentCaptor.getValue();
        capturedEntity.setTtl(actionEntity.getTtl()); //Questo campo non è replicabile nel test
        Assertions.assertEquals(actionEntity, capturedEntity);
        Mockito.verify(futureActionEntityDao, Mockito.times(1)).put(futureActionEntity);
    }



    @Test
    @ExtendWith(SpringExtension.class)
    void getActionById() {
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);
        ActionEntity actionEntity = buildActionEntity(action);
        Key keyToSearch = Key.builder()
                .partitionValue("2")
                .build();

        Mockito.when(actionEntityDao.get(keyToSearch)).thenReturn(Optional.of(actionEntity));

        Optional<Action> opt = dynamo.getActionById("2");

        Assertions.assertEquals(ActionType.ANALOG_WORKFLOW, opt.get().getType());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void findActionsByTimeSlot() {
        String timeslot = "2022-08-30T16:04:13.913859900Z";
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);
        ActionEntity actionEntity = buildActionEntity(action);
        FutureActionEntity futureActionEntity = buildFutureActionEntity(action, timeslot);

        Set<FutureActionEntity> entities = new HashSet<>();
        entities.add(futureActionEntity);

        Mockito.when(futureActionEntityDao.findByTimeSlot(timeslot)).thenReturn(entities);

        List<Action> actionList = dynamo.findActionsByTimeSlot(timeslot);

        Assertions.assertEquals(1, actionList.size());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void unSchedule() {
        String timeslot = "2022-08-30T16:04:13.913859900Z";
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);

        Key keyToDelete = buildKey(action, timeslot);

        dynamo.unSchedule(action, timeslot);

        Mockito.verify(futureActionEntityDao, Mockito.times(1)).delete(keyToDelete);
    }

    private Action buildAction(ActionType type) {

        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        return Action.builder()
                .iun("01")
                .actionId("02")
                .notBefore(instant)
                .type(type)
                .recipientIndex(3)
                .build();
    }

    private ActionEntity buildActionEntity(Action dto) {
        ActionEntity.ActionEntityBuilder builder = ActionEntity.builder()
                .actionId(dto.getActionId())
                .notBefore(dto.getNotBefore())
                .recipientIndex(dto.getRecipientIndex())
                .type(dto.getType())
                .iun(dto.getIun());
        return builder.build();
    }

    private FutureActionEntity buildFutureActionEntity(Action dto, String timeSlot) {
        FutureActionEntity.FutureActionEntityBuilder builder = FutureActionEntity.builder()
                .timeSlot(timeSlot)
                .actionId(dto.getActionId())
                .notBefore(dto.getNotBefore())
                .recipientIndex(dto.getRecipientIndex())
                .type(dto.getType())
                .iun(dto.getIun());
        return builder.build();
    }

    private Key buildKey(Action action, String timeSlot) {
        return Key.builder()
                .partitionValue(timeSlot)
                .sortValue(action.getActionId())
                .build();
    }

    private Action buildActionDto(ActionEntity entity) {
        Action.ActionBuilder builder = Action.builder()
                .actionId(entity.getActionId())
                .notBefore(entity.getNotBefore())
                .recipientIndex(entity.getRecipientIndex())
                .type(entity.getType())
                .iun(entity.getIun());
        return builder.build();
    }

    private Action buildFutureDto(FutureActionEntity entity) {
        Action.ActionBuilder builder = Action.builder()
                .actionId(entity.getActionId())
                .notBefore(entity.getNotBefore())
                .recipientIndex(entity.getRecipientIndex())
                .type(entity.getType())
                .iun(entity.getIun());
        return builder.build();
    }
}