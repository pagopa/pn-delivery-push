package it.pagopa.pn.deliverypush.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookForbiddenException;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookMaxStreamsCountReachedException;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamRequestV23;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.EventEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.WebhookStreamsService;
import it.pagopa.pn.deliverypush.service.utils.WebhookUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class WebhookStreamsServiceImplTest {
    Duration d = Duration.ofMillis(3000);

    private StreamEntityDao streamEntityDao;
    private EventEntityDao eventEntityDao;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private SchedulerService schedulerService;
    private WebhookStreamsService webhookService;
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

        MockitoAnnotations.initMocks(this);

        PnDeliveryPushConfigs.Webhook webhook = new PnDeliveryPushConfigs.Webhook();
        webhook.setScheduleInterval(1000L);
        webhook.setMaxLength(10);
        webhook.setPurgeDeletionWaittime(1000);
        webhook.setReadBufferDelay(1000);
        webhook.setMaxStreams(MAX_STREAMS);
        webhook.setTtl(Duration.ofDays(30));
        webhook.setFirstVersion("v10");
        webhook.setCurrentVersion("v23");
        Mockito.when(pnDeliveryPushConfigs.getWebhook()).thenReturn(webhook);

        webhookService = new WebhookStreamsServiceImpl(streamEntityDao, schedulerService,pnDeliveryPushConfigs
            ,pnExternalRegistryClient);
    }

    @Test
    void createEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


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
        StreamMetadataResponseV23 res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, null,null, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).save(Mockito.any());
    }


    @Test
    void createEventStreamMaxReached() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
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
        Mono<StreamMetadataResponseV23> mono = webhookService.createEventStream(xpagopapnuid, xpagopacxid,null,null, Mono.just(req));
        assertThrows(PnWebhookMaxStreamsCountReachedException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.never()).save(Mockito.any());
    }

    @Test
    void deleteEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType("");
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v23");

        Mockito.when(streamEntityDao.get(xpagopacxid,uuid)).thenReturn(Mono.just(entity));
        Mockito.when(streamEntityDao.delete(xpagopacxid, uuid)).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());


        //WHEN
        webhookService.deleteEventStream(xpagopapnuid,xpagopacxid, null,null,uuidd).block(d);

        //THEN
        Mockito.verify(streamEntityDao).delete(xpagopacxid, uuid);
    }

    @Test
    void deleteEventStreamNotAllowed(){
        deleteEventStreamException(null, null);
        deleteEventStreamException("v23", null);
        deleteEventStreamException("v23", "v10");
    }

    void deleteEventStreamException(String xPagopaPnApiVersion, String entityVersion) {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType("");
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion(entityVersion);

        Mockito.when(streamEntityDao.get(xpagopacxid,uuid)).thenReturn(Mono.just(entity));
        Mockito.when(streamEntityDao.delete(xpagopacxid, uuid)).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());


        //WHEN
        var mono = webhookService.deleteEventStream(xpagopapnuid,xpagopacxid, null,xPagopaPnApiVersion,uuidd);
        assertThrows(PnWebhookForbiddenException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.never()).delete(Mockito.any(), Mockito.any());
    }

    @Test
    void updateEventStreamNotAllowed() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
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


        Mono<StreamMetadataResponseV23> mono = webhookService.updateEventStream(xpagopapnuid,xpagopacxid,null,null, uuidd, Mono.just(req));
        assertThrows(PnWebhookForbiddenException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.never()).update(Mockito.any());
    }

    @Test
    void getEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


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
        StreamMetadataResponseV23 res = webhookService.getEventStream(xpagopapnuid,xpagopacxid,null,null, uuidd).block(d);

        //THEN
        assertNotNull(res);
        Mockito.verify(streamEntityDao).get(xpagopacxid, uuid);
    }

    @Test
    void listEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


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
        List<StreamListElement> res = webhookService.listEventStream(xpagopapnuid, xpagopacxid,null,null).collectList().block(d);

        //THEN
        assertNotNull(res);
        assertEquals(list.size(), res.size());
        Mockito.verify(streamEntityDao).findByPa(xpagopacxid);
    }

    @Test
    void updateEventStreamV10() {
        updateEventStream("v10",null);
    }

    @Test
    void updateEventStreamV23() {
        updateEventStream("v23","v23");
    }
    @Test
    void updateEventStreamDefault() {
        updateEventStream(null,"v23");
    }
    void updateEventStream(String xPagopaPnApiVersion, String entityVersion) {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

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
        entity.setVersion(entityVersion);


        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));


        //WHEN
        StreamMetadataResponseV23 res = webhookService.updateEventStream(xpagopapnuid,xpagopacxid, null,xPagopaPnApiVersion, uuidd, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).update(Mockito.any());
    }

    @Test
    void updateEventStreamForbidden() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
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
        Mono<StreamMetadataResponseV23> res = webhookService.updateEventStream(xpagopapnuid,xpagopacxid, null,null, uuidd, Mono.just(req));
        assertThrows(PnWebhookForbiddenException.class, () -> res.block(d));
        //THEN
        assertNotNull(res);

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





}