package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDto;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookForbiddenException;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookMaxStreamsCountReachedException;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequest;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponse;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.EventEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.EventEntityBatch;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.service.*;
import it.pagopa.pn.deliverypush.service.utils.WebhookUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WebhookServiceImplTest {

    Duration d = Duration.ofMillis(3000);

    private StreamEntityDao streamEntityDao;
    private EventEntityDao eventEntityDao;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private SchedulerService schedulerService;
    private WebhookService webhookService;
    private TimelineService timelineService;
    private NotificationService notificationService;
    private WebhookUtils webhookUtils;

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

        PnDeliveryPushConfigs.Webhook webhook = new PnDeliveryPushConfigs.Webhook();
        webhook.setScheduleInterval(1000L);
        webhook.setMaxLength(10);
        webhook.setPurgeDeletionWaittime(1000);
        webhook.setReadBufferDelay(1000);
        webhook.setMaxStreams(MAX_STREAMS);
        webhook.setTtl(Duration.ofDays(30));
        Mockito.when(pnDeliveryPushConfigs.getWebhook()).thenReturn(webhook);

        webhookService = new WebhookServiceImpl(streamEntityDao, eventEntityDao, pnDeliveryPushConfigs, schedulerService, webhookUtils);
    }

    @Test
    void createEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        StreamCreationRequest req = new StreamCreationRequest();
        req.setTitle("titolo");
        req.setEventType(StreamCreationRequest.EventTypeEnum.STATUS);
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
        StreamMetadataResponse res = webhookService.createEventStream(xpagopacxid, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).save(Mockito.any());
    }


    @Test
    void createEventStreamMaxReached() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        StreamCreationRequest req = new StreamCreationRequest();
        req.setTitle("titolo");
        req.setEventType(StreamCreationRequest.EventTypeEnum.STATUS);
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
        Mono<StreamMetadataResponse> mono = webhookService.createEventStream(xpagopacxid, Mono.just(req));
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
        webhookService.deleteEventStream(xpagopacxid, uuidd).block(d);

        //THEN
        Mockito.verify(streamEntityDao).delete(xpagopacxid, uuid);
    }

    @Test
    void updateEventStreamNotAllowed() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        StreamCreationRequest req = new StreamCreationRequest();
        req.setTitle("titolo");
        req.setEventType(StreamCreationRequest.EventTypeEnum.STATUS);
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
        Mockito.when(streamEntityDao.save(Mockito.any())).thenReturn(Mono.just(entity));


        Mono<StreamMetadataResponse> mono = webhookService.updateEventStream(xpagopacxid, uuidd, Mono.just(req));
        assertThrows(PnWebhookForbiddenException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.never()).save(Mockito.any());
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
        entity.setEventType(StreamMetadataResponse.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));


        //WHEN
        StreamMetadataResponse res = webhookService.getEventStream(xpagopacxid, uuidd).block(d);

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
        entity.setEventType(StreamMetadataResponse.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponse.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());

        List<StreamEntity> list = new ArrayList<>();
        list.add(entity);


        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));


        //WHEN
        List<StreamListElement> res = webhookService.listEventStream(xpagopacxid).collectList().block(d);

        //THEN
        assertNotNull(res);
        assertEquals(list.size(), res.size());
        Mockito.verify(streamEntityDao).findByPa(xpagopacxid);
    }

    @Test
    void updateEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        StreamCreationRequest req = new StreamCreationRequest();
        req.setTitle("titolo");
        req.setEventType(StreamCreationRequest.EventTypeEnum.STATUS);
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
        Mockito.when(streamEntityDao.save(Mockito.any())).thenReturn(Mono.just(entity));


        //WHEN
        StreamMetadataResponse res = webhookService.updateEventStream(xpagopacxid, uuidd, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).save(Mockito.any());
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
        entity.setEventType(StreamMetadataResponse.EventTypeEnum.STATUS.toString());
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
        ProgressResponseElementDto res = webhookService.consumeEventStream(xpagopacxid, uuidd, lasteventid).block(d);

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
        entity.setEventType(StreamMetadataResponse.EventTypeEnum.STATUS.toString());
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
        ProgressResponseElementDto res = webhookService.consumeEventStream(xpagopacxid, uuidd, lasteventid).block(d);

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
        Mono<ProgressResponseElementDto> mono = webhookService.consumeEventStream(xpagopacxid, uuidd, lasteventid);
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
        entity.setEventType(StreamMetadataResponse.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponse.EventTypeEnum.TIMELINE.toString());
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
        webhookService.saveEvent(xpagopacxid, newtimeline.getElementId() , eventEntity.getIun()).block(d);

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
        entity.setEventType(StreamMetadataResponse.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.getFilterValues().add(NotificationStatusInt.ACCEPTED.getValue());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponse.EventTypeEnum.STATUS.toString());
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
        webhookService.saveEvent(xpagopacxid, newtimeline.getElementId(), eventEntity.getIun() ).block(d);

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


        webhookService.saveEvent(xpagopacxid, newtimeline.getElementId(), eventEntity2.getIun()).block(d);

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
        entity.setEventType(StreamMetadataResponse.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.getFilterValues().add(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponse.EventTypeEnum.TIMELINE.toString());
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
        webhookService.saveEvent(xpagopacxid, newtimeline1.getElementId(), newtimeline1.getIun() ).block(d);

        webhookService.saveEvent(xpagopacxid, newtimeline2.getElementId(), newtimeline2.getIun() ).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(2)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(3)).save(Mockito.any(EventEntity.class));
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
        entity.setEventType(StreamMetadataResponse.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponse.EventTypeEnum.TIMELINE.toString());
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
        webhookService.saveEvent(xpagopacxid, newtimeline.getElementId(), newtimeline.getIun() ).block(d);

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
        webhookService.purgeEvents(xpagopacxid, lasteventid, true).block(d);

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
        webhookService.purgeEvents(xpagopacxid, lasteventid, true).block(d);

        //THEN
        Mockito.verify(eventEntityDao).delete(xpagopacxid, lasteventid, true);
        Mockito.verify(schedulerService, Mockito.times(1)).scheduleWebhookEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

    }
}