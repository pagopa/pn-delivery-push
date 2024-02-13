package it.pagopa.pn.deliverypush.service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookForbiddenException;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.DtoToEntityTimelineMapper;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.EntityToDtoTimelineMapper;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.TimelineElementJsonConverter;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WebhookUtilsTest {


    public static final String ERROR_CODE = "FILE_NOTFOUND";
    public static final String DETAIL = "Allegato non trovato. fileKey=81dde2a8-9719-4407-b7b3-63e7ea694869";
    private TimelineService timelineService;
    private StatusService statusService;
    private NotificationService notificationService;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private DtoToEntityTimelineMapper timelineMapper;
    private TimelineElementJsonConverter timelineElementJsonConverter;
    private ObjectMapper objectMapper;
    private EntityToDtoTimelineMapper entityToDtoTimelineMapper;

    private WebhookUtils webhookUtils;

    @BeforeEach
    void setup() {

        timelineService = Mockito.mock(TimelineService.class);
        notificationService = Mockito.mock(NotificationService.class);
        statusService = Mockito.mock(StatusService.class);
        pnDeliveryPushConfigs = Mockito.mock( PnDeliveryPushConfigs.class );
        timelineMapper = new DtoToEntityTimelineMapper();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        timelineElementJsonConverter = new TimelineElementJsonConverter(objectMapper);

        PnDeliveryPushConfigs.Webhook webhook = new PnDeliveryPushConfigs.Webhook();
        webhook.setScheduleInterval(1000L);
        webhook.setMaxLength(10);
        webhook.setPurgeDeletionWaittime(1000);
        webhook.setReadBufferDelay(1000);
        webhook.setMaxStreams(10);
        webhook.setTtl(Duration.ofDays(30));
        Mockito.when(pnDeliveryPushConfigs.getWebhook()).thenReturn(webhook);

        webhookUtils = new WebhookUtils(timelineService, statusService, notificationService, pnDeliveryPushConfigs, timelineMapper, entityToDtoTimelineMapper, timelineElementJsonConverter);
    }

    @Test
    void retrieveTimeline() {

        String iun = "IUN-ABC-123";
        String xpagopacxid = "PF-123456";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        Set<TimelineElementInternal> settimeline = new HashSet<>(timeline);
        Mockito.when(timelineService.getTimeline(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(settimeline);
        Mockito.when(statusService.computeStatusChange(Mockito.any(), Mockito.anySet(), Mockito.any())).thenReturn(Mockito.mock(StatusService.NotificationStatusUpdate.class));
        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(Mockito.mock(NotificationInt.class));

        WebhookUtils.RetrieveTimelineResult retrieveTimelineResult = webhookUtils.retrieveTimeline(iun, timeline.get(0).getElementId());

        assertNotNull(retrieveTimelineResult);
        assertEquals(retrieveTimelineResult.getEvent().getElementId(), timeline.get(0).getElementId());
        assertNotNull(retrieveTimelineResult.getNotificationStatusUpdate());
        assertNotNull(retrieveTimelineResult.getNotificationInt());
    }

    @Test
    void buildEventEntity() {

        String iun = "IUN-ABC-123";
        String xpagopacxid = "PF-123456";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal timelineElementInternal = timeline.get(2); //SEND_DIGITAL_DOMICILE
        StreamEntity streamEntity = new StreamEntity("paid", "abc");
        EventEntity eventEntity = webhookUtils.buildEventEntity(1L, streamEntity, "ACCEPTED", timelineElementInternal, NotificationInt.builder().build());

        assertNotNull(eventEntity);
        assertEquals(StringUtils.leftPad("1", 38, "0"), eventEntity.getEventId());
        assertNotNull(eventEntity.getElement());
    }

    @Test
    void buildEventEntity_2() {

        String iun = "IUN-ABC-123";
        String xpagopacxid = "PF-123456";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal timelineElementInternal = timeline.get(1);          //AAR_GENERATION
        StreamEntity streamEntity = new StreamEntity("paid", "abc");
        EventEntity eventEntity = webhookUtils.buildEventEntity(1L, streamEntity, "ACCEPTED", timelineElementInternal, NotificationInt.builder().build());

        assertNotNull(eventEntity);
        assertEquals(StringUtils.leftPad("1", 38, "0"), eventEntity.getEventId());
        assertNotNull(eventEntity.getElement());
        assertNotNull(eventEntity.getTtl());
    }


    @Test
    void buildEventEntity_3() {

        String iun = "IUN-ABC-123";
        String xpagopacxid = "PF-123456";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal timelineElementInternal = timeline.get(3);          //SEND_ANALOG_DOMICILE
        StreamEntity streamEntity = new StreamEntity("paid", "abc");
        NotificationInt notificationInt = NotificationInt.builder()
                .physicalCommunicationType(ServiceLevelTypeInt.REGISTERED_LETTER_890)
                .build();
        EventEntity eventEntity = webhookUtils.buildEventEntity(1L, streamEntity, "ACCEPTED", timelineElementInternal, notificationInt);

        assertNotNull(eventEntity);
        assertEquals(StringUtils.leftPad("1", 38, "0"), eventEntity.getEventId());
        assertNotNull(eventEntity.getElement());
        assertNotNull(eventEntity.getTtl());
    }


    @Test
    void buildEventEntity_4() {

        String iun = "IUN-ABC-123";
        String xpagopacxid = "PF-123456";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal timelineElementInternal = timeline.get(4);          //SEND_SIMPLE_REGISTERED_LETTER
        StreamEntity streamEntity = new StreamEntity("paid", "abc");
        NotificationInt notificationInt = NotificationInt.builder()
                .physicalCommunicationType(ServiceLevelTypeInt.REGISTERED_LETTER_890)
                .build();
        EventEntity eventEntity = webhookUtils.buildEventEntity(1L, streamEntity, "ACCEPTED", timelineElementInternal, notificationInt);

        assertNotNull(eventEntity);
        assertEquals(StringUtils.leftPad("1", 38, "0"), eventEntity.getEventId());
        assertNotNull(eventEntity.getElement());
        assertNotNull(eventEntity.getTtl());
    }


    @Test
    void buildEventEntity_5() {

        String iun = "IUN-ABC-123";
        String xpagopacxid = "PF-123456";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal timelineElementInternal = timeline.get(5);          //SEND_SIMPLE_REGISTERED_LETTER
        StreamEntity streamEntity = new StreamEntity("paid", "abc");
        NotificationInt notificationInt = NotificationInt.builder()
                .physicalCommunicationType(ServiceLevelTypeInt.REGISTERED_LETTER_890)
                .build();
        EventEntity eventEntity = webhookUtils.buildEventEntity(1L, streamEntity, "ACCEPTED", timelineElementInternal, notificationInt);

        assertNotNull(eventEntity);
        assertEquals(StringUtils.leftPad("1", 38, "0"), eventEntity.getEventId());
        assertNotNull(eventEntity.getElement());
        assertNotNull(eventEntity.getTtl());
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
                .legalFactsIds(List.of(LegalFactsIdInt.builder().category(LegalFactCategoryInt.SENDER_ACK).key("KEY1").build(), LegalFactsIdInt.builder().category(LegalFactCategoryInt.SENDER_ACK).key("KEY2").build()))
                .iun(iun)
                .elementId(iun + "_" + TimelineElementCategoryInt.AAR_GENERATION )
                .timestamp(t0.plusMillis(1000))
                        .details(AarGenerationDetailsInt.builder()
                                .recIndex(1)
                                .build())
                .paId(paId)
                .build());
        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .legalFactsIds(List.of(LegalFactsIdInt.builder().category(LegalFactCategoryInt.PEC_RECEIPT).key("KEY1").build()))
                .iun(iun)
                .elementId(iun + "_" + TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE )
                .timestamp(t0.plusMillis(1000))
                .details(SendDigitalDetailsInt.builder()
                        .recIndex(1)
                                .build())
                .paId(paId)
                .build());
        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .legalFactsIds(List.of(LegalFactsIdInt.builder().category(LegalFactCategoryInt.PEC_RECEIPT).key("KEY1").build()))
                .iun(iun)
                .elementId(iun + "_" + TimelineElementCategoryInt.SEND_ANALOG_DOMICILE )
                .timestamp(t0.plusMillis(1000))
                .details(SendAnalogDetailsInt.builder()
                        .recIndex(1)
                        .analogCost(500)
                        .build())
                .paId(paId)
                .build());

        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER)
                .iun(iun)
                .elementId(iun + "_" + TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER )
                .timestamp(t0.plusMillis(1000))
                .details(SimpleRegisteredLetterDetailsInt.builder()
                        .recIndex(1)
                        .analogCost(500)
                        .build())
                .paId(paId)
                .build());

        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE)
                .iun(iun)
                .elementId(iun + "_" + TimelineElementCategoryInt.SEND_COURTESY_MESSAGE )
                .timestamp(t0.plusMillis(1000))
                .details(SendCourtesyMessageDetailsInt.builder()
                        .recIndex(1)
                        .digitalAddress(CourtesyDigitalAddressInt.builder()
                                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                                .address("secret")
                                .build() )
                        .build())
                .paId(paId)
                .build());

        return res;
    }

    @Test
    void testVerifyVersion() {
        StreamEntity entity = new StreamEntity();
        entity.setVersion("v10");
        entity.setGroups(List.of("ADMINS"));

        // caso v10 stream e v10 api => OK
        Mono<StreamEntity> result = webhookUtils.verifyVersion(new ArrayList<>(), "v10", entity);

        StepVerifier.create(result)
                .expectNext(entity)
                .verifyComplete();

        // caso null stream e v23 api => OK
        entity.setVersion(null);
        result = webhookUtils.verifyVersion(List.of("ADMINS"), "v23", entity);

        StepVerifier.create(result)
                .expectNext(entity)
                .verifyComplete();

        // caso V23 stream e v23 api => OK
        entity.setVersion("v23");
        result = webhookUtils.verifyVersion(List.of("ADMINS"), "v23", entity);

        StepVerifier.create(result)
                .expectNext(entity)
                .verifyComplete();

        // caso V24 stream e v23 api, Group api is empty => OK
        entity.setVersion("v24");
        result = webhookUtils.verifyVersion(new ArrayList<>(), "v23", entity);

        StepVerifier.create(result)
                .expectNext(entity)
                .verifyComplete();

        // caso V24 stream e v23 api, Group api is equals stream => OK
        entity.setVersion("v24");
        result = webhookUtils.verifyVersion(List.of("ADMINS"), "v23", entity);

        StepVerifier.create(result)
                .expectNext(entity)
                .verifyComplete();

        // caso v10 stream e v23 api => Exception
        entity.setVersion("v10");
        result = webhookUtils.verifyVersion(new ArrayList<>(), "v23", entity);

        StepVerifier.create(result)
                .verifyError(PnWebhookForbiddenException.class);

        // caso v23 stream e v10 api => Exception
        entity.setVersion("v23");
        result = webhookUtils.verifyVersion(new ArrayList<>(), "v10", entity);

        StepVerifier.create(result)
                .verifyError(PnWebhookForbiddenException.class);

        // caso null stream e v10 api => Exception
        entity.setVersion(null);
        result = webhookUtils.verifyVersion(new ArrayList<>(), "v10", entity);

        StepVerifier.create(result)
                .verifyError(PnWebhookForbiddenException.class);

        // caso null stream e v23 api but groups is different => Exception
        entity.setVersion(null);
        result = webhookUtils.verifyVersion(new ArrayList<>(), "v23", entity);

        StepVerifier.create(result)
                .verifyError(PnWebhookForbiddenException.class);


        // caso v24 stream e v23 api but groups is different => Exception
        entity.setVersion("v24");
        result = webhookUtils.verifyVersion(List.of("TEST"), "v23", entity);

        StepVerifier.create(result)
                .verifyError(PnWebhookForbiddenException.class);



    }

}