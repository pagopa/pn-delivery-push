package it.pagopa.pn.deliverypush.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDto;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookForbiddenException;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookMaxStreamsCountReachedException;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamRequestV23;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.EventEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.EventEntityBatch;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.WebhookEventsService;
import it.pagopa.pn.deliverypush.service.WebhookStreamsService;
import it.pagopa.pn.deliverypush.service.utils.WebhookUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Disabled("IVAN")
class WebhookStreamsServiceImplTest {
    Duration d = Duration.ofMillis(3000);

    private StreamEntityDao streamEntityDao;
    private EventEntityDao eventEntityDao;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private SchedulerService schedulerService;
    private WebhookStreamsService webhookService;
    private WebhookEventsService webhookEventsService;
    private TimelineService timelineService;
    private NotificationService notificationService;
    private WebhookUtils webhookUtils;
    private PnExternalRegistryClient pnExternalRegistryClient;

    private final int MAX_STREAMS = 5;

    @BeforeEach
    void setup() {
        streamEntityDao = Mockito.mock( StreamEntityDao.class );
        eventEntityDao = Mockito.mock( EventEntityDao.class );
        pnDeliveryPushConfigs = Mockito.mock( PnDeliveryPushConfigs.class );
        schedulerService = Mockito.mock(SchedulerService.class);
        timelineService = Mockito.mock(TimelineService.class);
        notificationService = Mockito.mock(NotificationService.class);
        webhookUtils = Mockito.mock(WebhookUtils.class);
        pnExternalRegistryClient = Mockito.mock(PnExternalRegistryClient.class);

        PnDeliveryPushConfigs.Webhook webhook = new PnDeliveryPushConfigs.Webhook();
        webhook.setScheduleInterval(1000L);
        webhook.setMaxLength(10);
        webhook.setPurgeDeletionWaittime(1000);
        webhook.setReadBufferDelay(1000);
        webhook.setMaxStreams(MAX_STREAMS);
        webhook.setTtl(Duration.ofDays(30));
        Mockito.when(pnDeliveryPushConfigs.getWebhook()).thenReturn(webhook);

        webhookService = new WebhookStreamsServiceImpl(streamEntityDao, schedulerService,pnDeliveryPushConfigs
            ,pnExternalRegistryClient);
    }

    @Test
    void createEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        StreamCreationRequestV23 req = new StreamCreationRequestV23();
        req.setTitle("titolo");
        req.setEventType(StreamCreationRequestV23.EventTypeEnum.STATUS);
        req.setFilterValues(null);

        String uuid = UUID.randomUUID().toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());

        StreamEntity pentity = new StreamEntity();
        pentity.setStreamId(uuid);
        pentity.setTitle(req.getTitle());
        pentity.setPaId(xpagopacxid);
        pentity.setEventType(req.getEventType().toString());
        pentity.setFilterValues(new HashSet<>());
        pentity.setActivationDate(Instant.now());


        Mockito.when(streamEntityDao.findByPa(Mockito.anyString())).thenReturn(Flux.fromIterable(List.of(pentity)));
        Mockito.when(streamEntityDao.save(Mockito.any())).thenReturn(Mono.just(entity));


        //WHEN
        StreamMetadataResponseV23 res = webhookService.createEventStream(xpagopacxid, null,null, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).save(Mockito.any());
    }


    @Test
    void createEventStreamMaxReached() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        StreamCreationRequestV23 req = new StreamCreationRequestV23();
        req.setTitle("titolo");
        req.setEventType(StreamCreationRequestV23.EventTypeEnum.STATUS);
        req.setFilterValues(null);

        String uuid = UUID.randomUUID().toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());

        List<StreamEntity> sss = new ArrayList<>();
        for(int i = 0;i<MAX_STREAMS;i++) {
            StreamEntity pentity = new StreamEntity();
            pentity.setStreamId(UUID.randomUUID().toString());
            pentity.setTitle(req.getTitle());
            pentity.setPaId(xpagopacxid);
            pentity.setEventType(req.getEventType().toString());
            pentity.setFilterValues(new HashSet<>());
            pentity.setActivationDate(Instant.now());
            sss.add(pentity);
        }

        Mockito.when(streamEntityDao.findByPa(Mockito.anyString())).thenReturn(Flux.fromIterable(sss));
        Mockito.when(streamEntityDao.save(Mockito.any())).thenReturn(Mono.just(entity));

        //WHEN
        Mono<StreamMetadataResponseV23> mono = webhookService.createEventStream(xpagopacxid,null,null, Mono.just(req));
        assertThrows(PnWebhookMaxStreamsCountReachedException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.never()).save(Mockito.any());
    }

    @Test
    void deleteEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType("");
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        Mockito.when(streamEntityDao.delete(xpagopacxid, uuid)).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());


        //WHEN
        webhookService.deleteEventStream(xpagopacxid, null,null,uuidd).block(d);

        //THEN
        Mockito.verify(streamEntityDao).delete(xpagopacxid, uuid);
    }

    @Test
    void updateEventStreamNotAllowed() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        StreamRequestV23 req = new StreamRequestV23();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV23.EventTypeEnum.STATUS);
        req.setFilterValues(null);

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.empty());
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));


        Mono<StreamMetadataResponseV23> mono = webhookService.updateEventStream(xpagopacxid,null,null, uuidd, Mono.just(req));
        assertThrows(PnWebhookForbiddenException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.never()).update(Mockito.any());
    }

    @Test
    void getEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));


        //WHEN
        StreamMetadataResponseV23 res = webhookService.getEventStream(xpagopacxid,null,null, uuidd).block(d);

        //THEN
        assertNotNull(res);
        Mockito.verify(streamEntityDao).get(xpagopacxid, uuid);
    }

    @Test
    void listEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());

        List<StreamEntity> list = new ArrayList<>();
        list.add(entity);


        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));


        //WHEN
        List<StreamListElement> res = webhookService.listEventStream(xpagopacxid,null,null).collectList().block(d);

        //THEN
        assertNotNull(res);
        assertEquals(list.size(), res.size());
        Mockito.verify(streamEntityDao).findByPa(xpagopacxid);
    }

    @Test
    void updateEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        StreamRequestV23 req = new StreamRequestV23();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV23.EventTypeEnum.STATUS);
        req.setFilterValues(null);

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));


        //WHEN
        StreamMetadataResponseV23 res = webhookService.updateEventStream(xpagopacxid, null,null, uuidd, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).update(Mockito.any());
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
        //        entity.getFilterValues().add(TimelineElementCategoryInt.AAR_GENERATION.getValue());
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
    void saveEventNothingToDo() {
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
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV23.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(Set.of(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW.getValue()));
        entity.setActivationDate(Instant.now());

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
        NotificationInt notificationInt = NotificationInt.builder().build();

        StatusService.NotificationStatusUpdate  statusUpdate = Mockito.mock(StatusService.NotificationStatusUpdate.class);
        NotificationStatusInt notificationStatusInt = NotificationStatusInt.ACCEPTED;
        NotificationStatusInt notificationStatusInt1 = NotificationStatusInt.ACCEPTED;
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
}