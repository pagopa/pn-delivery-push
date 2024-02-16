package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDto;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookForbiddenException;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV23;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.EventEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.EventEntityBatch;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.utils.WebhookUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebhookEventsServiceImplTest {
    @InjectMocks
    private WebhookEventsServiceImpl webhookEventsService;
    @Mock
    private EventEntityDao eventEntityDao;
    @Mock
    private StreamEntityDao streamEntityDao;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private WebhookUtils webhookUtils;
    @Mock
    private TimelineService timelineService;
    @Mock
    private NotificationService notificationService;
    Duration d = Duration.ofSeconds(3);

    @BeforeEach
    void setup() {
        PnDeliveryPushConfigs.Webhook webhook = new PnDeliveryPushConfigs.Webhook();
        webhook.setScheduleInterval(1000L);
        webhook.setMaxLength(10);
        webhook.setPurgeDeletionWaittime(1000);
        webhook.setReadBufferDelay(1000);
        webhook.setTtl(Duration.ofDays(30));
        Mockito.when(pnDeliveryPushConfigs.getWebhook()).thenReturn(webhook);

        Set<String> listCategoriesPa = new HashSet<>(List.of("REQUEST_REFUSED", "REQUEST_ACCEPTED", "SEND_DIGITAL_DOMICILE", "SEND_DIGITAL_FEEDBACK",
                "DIGITAL_SUCCESS_WORKFLOW", "DIGITAL_FAILURE_WORKFLOW", "SEND_SIMPLE_REGISTERED_LETTER", "SEND_SIMPLE_REGISTERED_LETTER_PROGRESS",
                "SEND_ANALOG_DOMICILE", "SEND_ANALOG_PROGRESS", "SEND_ANALOG_FEEDBACK", "ANALOG_SUCCESS_WORKFLOW", "ANALOG_FAILURE_WORKFLOW",
                "COMPLETELY_UNREACHABLE", "REFINEMENT", "NOTIFICATION_VIEWED", "NOTIFICATION_CANCELLED", "NOTIFICATION_RADD_RETRIEVED"));
        Mockito.when(pnDeliveryPushConfigs.getListCategoriesPa()).thenReturn(listCategoriesPa);
    }

    private List<TimelineElementInternal> generateTimeline(String iun, String paId){
        List<TimelineElementInternal> res = new ArrayList<>();
        Instant t0 = Instant.now();

        res.add(TimelineElementInternal.builder()
            .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
            .iun(iun)
            .elementId(iun + "_" + TimelineElementCategoryInt.REQUEST_ACCEPTED )
            .timestamp(t0)
            .paId(paId)
            .build());
        res.add(TimelineElementInternal.builder()
            .category(TimelineElementCategoryInt.AAR_GENERATION)
            .iun(iun)
            .elementId(iun + "_" + TimelineElementCategoryInt.AAR_GENERATION )
            .timestamp(t0.plusMillis(1000))
            .paId(paId)
            .build());
        res.add(TimelineElementInternal.builder()
            .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
            .iun(iun)
            .elementId(iun + "_" + TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE )
            .timestamp(t0.plusMillis(1000))
            .paId(paId)
            .build());

        return res;
    }

    @Test
    void saveEventNothingToDo() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-ABC-FGHI-A-1";
        String authGroup = "PA-groupID";

        List<String> groupsList = new ArrayList<>();
        groupsList.add(authGroup);

        List<StreamEntity> list = new ArrayList<>();
        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setGroups(groupsList);
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(Set.of(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW.getValue()));
        entity.setActivationDate(Instant.now());
        entity.setGroups(groupsList);
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        Set<TimelineElementInternal> settimeline = new HashSet<>(timeline);
        TimelineElementInternal newtimeline = timeline.get(timeline.size()-1);
        NotificationInt notificationInt = NotificationInt.builder()
                .group(authGroup)
                .build();

        StatusService.NotificationStatusUpdate  statusUpdate = Mockito.mock(StatusService.NotificationStatusUpdate.class);
        NotificationStatusInt notificationStatusInt = NotificationStatusInt.ACCEPTED;
        NotificationStatusInt notificationStatusInt1 = NotificationStatusInt.ACCEPTED;
        Mockito.when(statusUpdate.getNewStatus()).thenReturn(notificationStatusInt1);
        Mockito.when(statusUpdate.getOldStatus()).thenReturn(notificationStatusInt);

        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.REQUEST_ACCEPTED);

        WebhookUtils.RetrieveTimelineResult retrieveTimelineResult = WebhookUtils.RetrieveTimelineResult.builder()
            .notificationInt(notificationInt)
            .event(timelineElementInternal)
            .notificationStatusUpdate(statusUpdate)
            .build();

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.retrieveTimeline(Mockito.anyString(), Mockito.anyString())).thenReturn(retrieveTimelineResult);


        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(timelineService.getTimeline(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(settimeline);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);



        //WHEN
        webhookEventsService.saveEvent(xpagopacxid, newtimeline.getElementId(), newtimeline.getIun() ).block(d);

        //THEN
        Mockito.verify(streamEntityDao).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(0)).save(Mockito.any(EventEntity.class));
    }


    @Test
    void purgeEvents() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String lasteventid = "lasteventid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType("");
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        Mockito.when(eventEntityDao.delete(xpagopacxid, lasteventid, true)).thenReturn(Mono.just(false));
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());


        //WHEN
        webhookEventsService.purgeEvents(xpagopacxid, lasteventid, true).block(d);

        //THEN
        Mockito.verify(eventEntityDao).delete(xpagopacxid, lasteventid, true);
        Mockito.verify(schedulerService, Mockito.never()).scheduleWebhookEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

    }
    @Test
    void purgeEventsWithRetry() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String lasteventid = "lasteventid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType("");
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        Mockito.when(eventEntityDao.delete(xpagopacxid, lasteventid, true))
            .thenReturn(Mono.just(true)).thenReturn(Mono.just(false));
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());


        //WHEN
        webhookEventsService.purgeEvents(xpagopacxid, lasteventid, true).block(d);

        //THEN
        Mockito.verify(eventEntityDao).delete(xpagopacxid, lasteventid, true);
        Mockito.verify(schedulerService, Mockito.times(1)).scheduleWebhookEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

    }
    @Test
    void consumeEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String lasteventid = null;


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        List<EventEntity> list = new ArrayList<>();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);



        eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now().plusMillis(1) + "_" + "timeline_event_id2");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);

        EventEntityBatch eventEntityBatch = new EventEntityBatch();
        eventEntityBatch.setEvents(list);
        eventEntityBatch.setStreamId(uuid);
        eventEntityBatch.setLastEventIdRead(null);



        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.when(eventEntityDao.findByStreamId(uuid, lasteventid)).thenReturn(Mono.just(eventEntityBatch));


        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid,null,null, uuidd, lasteventid).block(d);

        //THEN
        assertNotNull(res);
        assertEquals(list.size(), res.getProgressResponseElementList().size());
        Mockito.verify(streamEntityDao).get(xpagopacxid, uuid);
        Mockito.verify(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
    }



    @Test
    void consumeEventStreamNearEvents() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String lasteventid;


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        List<EventEntity> list = new ArrayList<>();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);



        eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now().plusMillis(1) + "_" + "timeline_event_id2");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);

        EventEntityBatch eventEntityBatch = new EventEntityBatch();
        eventEntityBatch.setEvents(list);
        eventEntityBatch.setStreamId(uuid);
        eventEntityBatch.setLastEventIdRead(null);


        lasteventid = list.get(0).getEventId();

        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.when(eventEntityDao.findByStreamId(Mockito.anyString() , Mockito.anyString())).thenReturn(Mono.just(eventEntityBatch));


        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid,null,null, uuidd, lasteventid).block(d);

        //THEN
        assertNotNull(res);
        assertEquals(list.size(), res.getProgressResponseElementList().size());
        Mockito.verify(streamEntityDao).get(xpagopacxid, uuid);
        Mockito.verify(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
    }


    @Test
    void consumeEventStreamNotAllowed() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String lasteventid = null;

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();


        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.when(eventEntityDao.findByStreamId(uuid, lasteventid)).thenReturn(Mono.empty());


        //WHEN
        Mono<ProgressResponseElementDto> mono = webhookEventsService.consumeEventStream(xpagopacxid, null,null,uuidd, lasteventid);
        assertThrows(PnWebhookForbiddenException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(eventEntityDao, Mockito.never()).findByStreamId(Mockito.anyString(), Mockito.any());
        Mockito.verify(schedulerService, Mockito.never()).scheduleWebhookEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

    }

    @Test
    void saveEvent() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-ABC-FGHI-A-1";

        List<StreamEntity> list = new ArrayList<>();
        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(2L);
        entity.setVersion("V10");
        list.add(entity);


        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        Set<TimelineElementInternal> settimeline = new HashSet<>(timeline);
        TimelineElementInternal newtimeline = timeline.get(timeline.size()-1);
        NotificationInt notificationInt = NotificationInt.builder().build();
        StatusService.NotificationStatusUpdate notificationStatusUpdate = new
            StatusService.NotificationStatusUpdate(NotificationStatusInt.ACCEPTED, NotificationStatusInt.DELIVERING);

        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + newtimeline.getElementId());
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.DELIVERING.getValue());
        eventEntity.setIun(iun);
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        StatusService.NotificationStatusUpdate  statusUpdate = Mockito.mock(StatusService.NotificationStatusUpdate.class);
        NotificationStatusInt notificationStatusInt = NotificationStatusInt.ACCEPTED;
        NotificationStatusInt notificationStatusInt1 = NotificationStatusInt.DELIVERING;
        Mockito.when(statusUpdate.getNewStatus()).thenReturn(notificationStatusInt);
        Mockito.when(statusUpdate.getOldStatus()).thenReturn(notificationStatusInt1);

        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.REQUEST_ACCEPTED);

        WebhookUtils.RetrieveTimelineResult retrieveTimelineResult = WebhookUtils.RetrieveTimelineResult.builder()
            .notificationInt(Mockito.mock(NotificationInt.class))
            .event(timelineElementInternal)
            .notificationStatusUpdate(statusUpdate)
            .build();

        Mockito.when(webhookUtils.getVersion("V10")).thenReturn(10);
        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.retrieveTimeline(Mockito.anyString(), Mockito.anyString())).thenReturn(retrieveTimelineResult);

        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(1))).thenReturn(Mono.just(3L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(timelineService.getTimeline(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(settimeline);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);

        //WHEN
        webhookEventsService.saveEvent(xpagopacxid, newtimeline.getElementId() , eventEntity.getIun()).block(d);
        //THEN
        Mockito.verify(streamEntityDao).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(list.size())).save(Mockito.any(EventEntity.class));
    }


    @Test
    void saveEventFiltered() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-ABC-FGHI-A-1";


        List<StreamEntity> list = new ArrayList<>();
        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.getFilterValues().add(NotificationStatusInt.ACCEPTED.getValue());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(2L);
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        EventEntity eventEntity2 = new EventEntity();
        eventEntity2.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity2.setTimestamp(Instant.now());
        eventEntity2.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity2.setNewStatus(NotificationStatusInt.DELIVERED.getValue());
        eventEntity2.setIun("");
        eventEntity2.setNotificationRequestId("");
        eventEntity2.setStreamId(uuid);

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        Set<TimelineElementInternal> settimeline = new HashSet<>(timeline);
        TimelineElementInternal newtimeline = timeline.get(timeline.size()-1);
        NotificationInt notificationInt = NotificationInt.builder().build();
        StatusService.NotificationStatusUpdate notificationStatusUpdate = new
            StatusService.NotificationStatusUpdate(NotificationStatusInt.ACCEPTED, NotificationStatusInt.DELIVERING);

        StatusService.NotificationStatusUpdate  statusUpdate = Mockito.mock(StatusService.NotificationStatusUpdate.class);
        NotificationStatusInt notificationStatusInt = NotificationStatusInt.ACCEPTED;
        NotificationStatusInt notificationStatusInt1 = NotificationStatusInt.DELIVERING;
        Mockito.when(statusUpdate.getNewStatus()).thenReturn(notificationStatusInt1);
        Mockito.when(statusUpdate.getOldStatus()).thenReturn(notificationStatusInt);

        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.REQUEST_ACCEPTED);

        WebhookUtils.RetrieveTimelineResult retrieveTimelineResult = WebhookUtils.RetrieveTimelineResult.builder()
            .notificationInt(Mockito.mock(NotificationInt.class))
            .event(timelineElementInternal)
            .notificationStatusUpdate(statusUpdate)
            .build();
        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.retrieveTimeline(Mockito.anyString(), Mockito.anyString())).thenReturn(retrieveTimelineResult);

        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(1))).thenReturn(Mono.just(3L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(timelineService.getTimeline(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(settimeline);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);


        //WHEN
        webhookEventsService.saveEvent(xpagopacxid, newtimeline.getElementId(), eventEntity.getIun() ).block(d);

        // altro test
        statusUpdate = Mockito.mock(StatusService.NotificationStatusUpdate.class);
        notificationStatusInt = NotificationStatusInt.IN_VALIDATION;
        notificationStatusInt1 = NotificationStatusInt.ACCEPTED;
        Mockito.when(statusUpdate.getNewStatus()).thenReturn(notificationStatusInt1);
        Mockito.when(statusUpdate.getOldStatus()).thenReturn(notificationStatusInt);

        timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.REQUEST_ACCEPTED);

        retrieveTimelineResult = WebhookUtils.RetrieveTimelineResult.builder()
            .notificationInt(Mockito.mock(NotificationInt.class))
            .event(timelineElementInternal)
            .notificationStatusUpdate(statusUpdate)
            .build();
        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.retrieveTimeline(Mockito.anyString(), Mockito.anyString())).thenReturn(retrieveTimelineResult);


        webhookEventsService.saveEvent(xpagopacxid, newtimeline.getElementId(), eventEntity2.getIun()).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(2)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(3)).save(Mockito.any(EventEntity.class));
    }

    @Test
    void saveEventFilteredTimeline() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-ABC-FGHI-A-1";

        List<StreamEntity> list = new ArrayList<>();
        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.getFilterValues().add(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(2L);
        entity.setVersion("V23");
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        EventEntity eventEntity2 = new EventEntity();
        eventEntity2.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity2.setTimestamp(Instant.now());
        eventEntity2.setTimelineEventCategory(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW.getValue());
        eventEntity2.setNewStatus(NotificationStatusInt.DELIVERED.getValue());
        eventEntity2.setIun("");
        eventEntity2.setNotificationRequestId("");
        eventEntity2.setStreamId(uuid);

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        Set<TimelineElementInternal> settimeline = new HashSet<>(timeline);
        TimelineElementInternal newtimeline1 = timeline.get(timeline.size()-1);
        TimelineElementInternal newtimeline2 = timeline.get(timeline.size()-2);
        NotificationInt notificationInt = NotificationInt.builder().build();


        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.AAR_GENERATION);


        TimelineElementInternal timelineElementInternal2 = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal2.getCategory()).thenReturn(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE);


        StatusService.NotificationStatusUpdate notificationStatusUpdate = new
            StatusService.NotificationStatusUpdate(NotificationStatusInt.ACCEPTED, NotificationStatusInt.DELIVERING);
        WebhookUtils.RetrieveTimelineResult retrieveTimelineResult2 = WebhookUtils.RetrieveTimelineResult.builder()
            .notificationInt(notificationInt)
            .event(timelineElementInternal2)
            .notificationStatusUpdate(notificationStatusUpdate)
            .build();


        WebhookUtils.RetrieveTimelineResult retrieveTimelineResult = WebhookUtils.RetrieveTimelineResult.builder()
            .notificationInt(notificationInt)
            .event(timelineElementInternal)
            .notificationStatusUpdate(notificationStatusUpdate)
            .build();

        Mockito.when(webhookUtils.getVersion("V23")).thenReturn(10);

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.retrieveTimeline(newtimeline1.getIun() , newtimeline1.getElementId())).thenReturn(retrieveTimelineResult);

        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(1))).thenReturn(Mono.just(3L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(timelineService.getTimeline(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(settimeline);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(webhookUtils.retrieveTimeline(newtimeline2.getIun() , newtimeline2.getElementId())).thenReturn(retrieveTimelineResult2);


        //WHEN
        webhookEventsService.saveEvent(xpagopacxid, newtimeline1.getElementId(), newtimeline1.getIun() ).block(d);

        webhookEventsService.saveEvent(xpagopacxid, newtimeline2.getElementId(), newtimeline2.getIun() ).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(2)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(3)).save(Mockito.any(EventEntity.class));
    }


    @Test
    void saveEventFilteredTimelineV1() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-ABC-FGHI-A-1";

        List<StreamEntity> list = new ArrayList<>();
        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        entity.setVersion("V10");
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(2L);
        entity.setVersion("V10");
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        EventEntity eventEntity2 = new EventEntity();
        eventEntity2.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity2.setTimestamp(Instant.now());
        eventEntity2.setTimelineEventCategory(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW.getValue());
        eventEntity2.setNewStatus(NotificationStatusInt.DELIVERED.getValue());
        eventEntity2.setIun("");
        eventEntity2.setNotificationRequestId("");
        eventEntity2.setStreamId(uuid);

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        timeline.add(TimelineElementInternal.builder()
            .category(TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST)
            .iun(iun)
            .elementId(iun + "_" + TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST )
            .timestamp(Instant.now())
            .paId(xpagopacxid)
            .build());

        timeline.add(TimelineElementInternal.builder()
            .category(TimelineElementCategoryInt.NOTIFICATION_CANCELLED)
            .iun(iun)
            .elementId(iun + "_" + TimelineElementCategoryInt.NOTIFICATION_CANCELLED )
            .timestamp(Instant.now())
            .paId(xpagopacxid)
            .build());

        timeline.add(TimelineElementInternal.builder()
            .category(TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE)
            .iun(iun)
            .elementId(iun + "_" + TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE )
            .timestamp(Instant.now())
            .paId(xpagopacxid)
            .build());
        Set<TimelineElementInternal> settimeline = new HashSet<>(timeline);
        TimelineElementInternal newtimeline1 = timeline.get(0);
        TimelineElementInternal newtimeline2 = timeline.get(1);
        TimelineElementInternal newtimeline3 = timeline.get(2);
        TimelineElementInternal newtimeline4 = timeline.get(3);
        TimelineElementInternal newtimeline5 = timeline.get(4);

        NotificationInt notificationInt = NotificationInt.builder().build();


        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.AAR_GENERATION);


        TimelineElementInternal timelineElementInternal2 = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal2.getCategory()).thenReturn(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE);


        StatusService.NotificationStatusUpdate notificationStatusUpdate = new
            StatusService.NotificationStatusUpdate(NotificationStatusInt.ACCEPTED, NotificationStatusInt.DELIVERING);
        WebhookUtils.RetrieveTimelineResult retrieveTimelineResult2 = WebhookUtils.RetrieveTimelineResult.builder()
            .notificationInt(notificationInt)
            .event(timelineElementInternal2)
            .notificationStatusUpdate(notificationStatusUpdate)
            .build();


        WebhookUtils.RetrieveTimelineResult retrieveTimelineResult = WebhookUtils.RetrieveTimelineResult.builder()
            .notificationInt(notificationInt)
            .event(timelineElementInternal)
            .notificationStatusUpdate(notificationStatusUpdate)
            .build();

        Mockito.when(webhookUtils.getVersion("V10")).thenReturn(10);

        Mockito.doReturn(23)
                .when(webhookUtils)
                .getVersion("V23");

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.retrieveTimeline(newtimeline1.getIun() , newtimeline1.getElementId())).thenReturn(retrieveTimelineResult);

        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(1))).thenReturn(Mono.just(3L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(timelineService.getTimeline(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(settimeline);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(webhookUtils.retrieveTimeline(newtimeline2.getIun() , newtimeline2.getElementId())).thenReturn(retrieveTimelineResult2);

        TimelineElementInternal timelineElementInternal3 = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal3.getCategory()).thenReturn(TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST);
        WebhookUtils.RetrieveTimelineResult retrieveTimelineResult3 = WebhookUtils.RetrieveTimelineResult.builder()
            .notificationInt(notificationInt)
            .event(timelineElementInternal3)
            .notificationStatusUpdate(notificationStatusUpdate)
            .build();
        Mockito.when(webhookUtils.retrieveTimeline(newtimeline3.getIun() , newtimeline3.getElementId())).thenReturn(retrieveTimelineResult3);


        TimelineElementInternal timelineElementInternal4 = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal4.getCategory()).thenReturn(TimelineElementCategoryInt.NOTIFICATION_CANCELLED);
        WebhookUtils.RetrieveTimelineResult retrieveTimelineResult4 = WebhookUtils.RetrieveTimelineResult.builder()
            .notificationInt(notificationInt)
            .event(timelineElementInternal4)
            .notificationStatusUpdate(notificationStatusUpdate)
            .build();
        Mockito.when(webhookUtils.retrieveTimeline(newtimeline4.getIun() , newtimeline4.getElementId())).thenReturn(retrieveTimelineResult4);


        TimelineElementInternal timelineElementInternal5 = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal5.getCategory()).thenReturn(TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE);
        WebhookUtils.RetrieveTimelineResult retrieveTimelineResult5 = WebhookUtils.RetrieveTimelineResult.builder()
            .notificationInt(notificationInt)
            .event(timelineElementInternal5)
            .notificationStatusUpdate(notificationStatusUpdate)
            .build();
        Mockito.when(webhookUtils.retrieveTimeline(newtimeline5.getIun() , newtimeline5.getElementId())).thenReturn(retrieveTimelineResult5);

        //WHEN
        timeline.forEach(t -> {
            webhookEventsService.saveEvent(xpagopacxid, t.getElementId(), t.getIun() ).block(d);
        });

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(timeline.size())).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(4)).save(Mockito.any(EventEntity.class));
    }

    @Test
    void saveEventWhenGroupIsUnauthorizedOrWhenIsAuthorized() {
        //UNAUTHORIZED CASE
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-ABC-FGHI-A-1";
        String authGroup1 = "PA-1groupID";
        String authGroup2 = "PA-2groupID";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal newtimeline1 = timeline.get(timeline.size()-1);

        List<String> groupsList = new ArrayList<>();
        groupsList.add(authGroup1);

        List<StreamEntity> streamEntityList = new ArrayList<>();
        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity streamEntity = new StreamEntity();
        streamEntity.setStreamId(uuid);
        streamEntity.setStreamId(uuid);
        streamEntity.setTitle("1");
        streamEntity.setPaId(xpagopacxid);
        streamEntity.setEventType(StreamMetadataResponseV23.EventTypeEnum.TIMELINE.toString());
        streamEntity.setFilterValues(Set.of(TimelineElementCategoryInt.REQUEST_ACCEPTED.getValue()));
        streamEntity.setActivationDate(Instant.now());
        streamEntity.setEventAtomicCounter(1L);
        streamEntity.setGroups(groupsList);
        streamEntityList.add(streamEntity);

        Mockito.when(streamEntityDao.findByPa(xpagopacxid))
                .thenReturn(Flux.fromIterable(streamEntityList));

        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal.getCategory())
                .thenReturn(TimelineElementCategoryInt.REQUEST_ACCEPTED);

        StatusService.NotificationStatusUpdate notificationStatusUpdate = new
                StatusService.NotificationStatusUpdate(NotificationStatusInt.ACCEPTED, NotificationStatusInt.DELIVERING);

        NotificationInt notificationInt = NotificationInt.builder()
                .group(authGroup2)
                .build();

        WebhookUtils.RetrieveTimelineResult retrieveTimelineResult = WebhookUtils.RetrieveTimelineResult.builder()
                .notificationInt(notificationInt)
                .event(timelineElementInternal)
                .notificationStatusUpdate(notificationStatusUpdate)
                .build();
        Mockito.when(webhookUtils.retrieveTimeline(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(retrieveTimelineResult);

        //WHEN
        webhookEventsService.saveEvent(xpagopacxid, newtimeline1.getElementId(), newtimeline1.getIun())
                .block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1))
                .findByPa(xpagopacxid);



        //AUTHORIZED CASE
        groupsList.clear();
        groupsList.add(authGroup2);
        streamEntity.setGroups(groupsList);

        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(streamEntityList.get(0)))
                .thenReturn(Mono.just(2L));
        Mockito.when(eventEntityDao.save(Mockito.any()))
                .thenReturn(Mono.just(new EventEntity()));

        //WHEN
        webhookEventsService.saveEvent(xpagopacxid, newtimeline1.getElementId(), newtimeline1.getIun())
                .block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(2))
                .findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(1))
                .save(Mockito.any());
    }

    @Test
    void saveEventWhenFilteredValueIsDefaultCategoriesPA() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-ABC-FGHI-A-1";
        String authGroup = "PA-1groupID";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal newtimeline1 = timeline.get(timeline.size()-1);

        List<String> groupsList = new ArrayList<>();
        groupsList.add(authGroup);

        List<StreamEntity> streamEntityList = new ArrayList<>();
        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity streamEntity = new StreamEntity();
        streamEntity.setStreamId(uuid);
        streamEntity.setStreamId(uuid);
        streamEntity.setTitle("1");
        streamEntity.setPaId(xpagopacxid);
        streamEntity.setEventType(StreamMetadataResponseV23.EventTypeEnum.TIMELINE.toString());
        streamEntity.setFilterValues(Set.of("DEFAULT"));
        streamEntity.setActivationDate(Instant.now());
        streamEntity.setEventAtomicCounter(1L);
        streamEntity.setGroups(groupsList);
        streamEntityList.add(streamEntity);

        Mockito.when(streamEntityDao.findByPa(xpagopacxid))
                .thenReturn(Flux.fromIterable(streamEntityList));

        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal.getCategory())
                .thenReturn(TimelineElementCategoryInt.REQUEST_ACCEPTED);

        StatusService.NotificationStatusUpdate notificationStatusUpdate = new
                StatusService.NotificationStatusUpdate(NotificationStatusInt.ACCEPTED, NotificationStatusInt.DELIVERING);

        NotificationInt notificationInt = NotificationInt.builder()
                .group(authGroup)
                .build();

        WebhookUtils.RetrieveTimelineResult retrieveTimelineResult = WebhookUtils.RetrieveTimelineResult.builder()
                .notificationInt(notificationInt)
                .event(timelineElementInternal)
                .notificationStatusUpdate(notificationStatusUpdate)
                .build();
        Mockito.when(webhookUtils.retrieveTimeline(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(retrieveTimelineResult);

        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(streamEntityList.get(0)))
                .thenReturn(Mono.just(2L));

        Mockito.when(eventEntityDao.save(Mockito.any()))
                .thenReturn(Mono.just(new EventEntity()));

        //WHEN
        webhookEventsService.saveEvent(xpagopacxid, newtimeline1.getElementId(), newtimeline1.getIun())
                .block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1))
                .findByPa(xpagopacxid);
        Mockito.verify(streamEntityDao, Mockito.times(1))
                .updateAndGetAtomicCounter(Mockito.any());
        Mockito.verify(eventEntityDao, Mockito.times(1))
                .save(Mockito.any());
    }
}