package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class TimelineServiceFactoryTest {
    private TimelineServiceHttpImpl timelineServiceHttpImpl;
    private TimeLineServiceImpl timelineServiceImpl;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private TimelineServiceFactory factory;

    @BeforeEach
    void setUp() {
        timelineServiceHttpImpl = mock(TimelineServiceHttpImpl.class);
        timelineServiceImpl = mock(TimeLineServiceImpl.class);
        pnDeliveryPushConfigs = mock(PnDeliveryPushConfigs.class);
        factory = new TimelineServiceFactory(timelineServiceHttpImpl, timelineServiceImpl, pnDeliveryPushConfigs);
    }

    @Test
    void createTimelineServiceShouldCallWithCurrentInstant() {
        try (MockedStatic<Instant> mockedInstant = mockStatic(Instant.class)) {
            Instant now = Instant.parse("2024-06-01T12:00:00Z");
            mockedInstant.when(Instant::now).thenReturn(now);

            TimelineServiceFactory spyFactory = spy(factory);
            TimelineService expectedService = mock(TimelineService.class);
            doReturn(expectedService).when(spyFactory).createTimelineService(now);

            TimelineService result = spyFactory.createTimelineService();

            assertSame(expectedService, result);
            verify(spyFactory).createTimelineService(now);
        }
    }

    @Test
    void shouldReturnTimelineServiceImplWhenFeatureFlagIsNull() {
        when(pnDeliveryPushConfigs.getTimelineClientEnabledStartDate()).thenReturn(null);

        TimelineService result = factory.createTimelineService(Instant.now());

        assertSame(timelineServiceImpl, result);
    }

    @Test
    void shouldReturnTimelineServiceHttpImplWhenReferenceInstantAfterOrEqualsFeatureFlag() {
        Instant featureFlag = Instant.parse("2024-01-01T00:00:00Z");
        when(pnDeliveryPushConfigs.getTimelineClientEnabledStartDate()).thenReturn(featureFlag);

        TimelineService resultAfter = factory.createTimelineService(featureFlag.plusSeconds(1));
        TimelineService resultEquals = factory.createTimelineService(featureFlag);

        assertSame(timelineServiceHttpImpl, resultAfter);
        assertSame(timelineServiceHttpImpl, resultEquals);
    }

    @Test
    void shouldReturnTimelineServiceImplWhenReferenceInstantBeforeFeatureFlag() {
        Instant featureFlag = Instant.parse("2024-01-01T00:00:00Z");
        when(pnDeliveryPushConfigs.getTimelineClientEnabledStartDate()).thenReturn(featureFlag);

        TimelineService result = factory.createTimelineService(featureFlag.minusSeconds(1));

        assertSame(timelineServiceImpl, result);
    }
}