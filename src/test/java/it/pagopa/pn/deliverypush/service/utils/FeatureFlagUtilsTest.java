package it.pagopa.pn.deliverypush.service.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FeatureFlagUtilsTest {

    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private FeatureFlagUtils featureFlagUtils;

    @BeforeEach
    void setUp() {
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        featureFlagUtils = new FeatureFlagUtils(pnDeliveryPushConfigs);
    }

    @Test
    void isActionImplDao_returnsTrueWhenNowWithinRange() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2025-01-01T00:00:00Z");
        Instant now = Instant.parse("2024-06-01T12:00:00Z");

        Mockito.when(pnDeliveryPushConfigs.getStartActionImplWithDaoTimestamp()).thenReturn(start);
        Mockito.when(pnDeliveryPushConfigs.getEndActionImplWithDaoTimestamp()).thenReturn(end);

        assertTrue(featureFlagUtils.isActionImplDao(now));
    }

    @Test
    void isActionImplDao_returnsFalseWhenNowBeforeStart() {
        Instant start = Instant.parse("2024-07-01T00:00:00Z");
        Instant end = Instant.parse("2025-01-01T00:00:00Z");
        Instant now = Instant.parse("2024-06-01T12:00:00Z");

        Mockito.when(pnDeliveryPushConfigs.getStartActionImplWithDaoTimestamp()).thenReturn(start);
        Mockito.when(pnDeliveryPushConfigs.getEndActionImplWithDaoTimestamp()).thenReturn(end);

        assertFalse(featureFlagUtils.isActionImplDao(now));
    }

    @Test
    void isActionImplDao_returnsFalseWhenNowAfterEnd() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-06-01T00:00:00Z");
        Instant now = Instant.parse("2024-07-01T12:00:00Z");

        Mockito.when(pnDeliveryPushConfigs.getStartActionImplWithDaoTimestamp()).thenReturn(start);
        Mockito.when(pnDeliveryPushConfigs.getEndActionImplWithDaoTimestamp()).thenReturn(end);

        assertFalse(featureFlagUtils.isActionImplDao(now));
    }

    @Test
    void isActionImplDao_throwsExceptionWhenStartDateIsNull() {
        Instant end = Instant.parse("2025-01-01T00:00:00Z");
        Instant now = Instant.parse("2024-06-01T12:00:00Z");

        Mockito.when(pnDeliveryPushConfigs.getStartActionImplWithDaoTimestamp()).thenReturn(null);
        Mockito.when(pnDeliveryPushConfigs.getEndActionImplWithDaoTimestamp()).thenReturn(end);

        assertThrows(PnInternalException.class, () -> featureFlagUtils.isActionImplDao(now));
    }

    @Test
    void isActionImplDao_throwsExceptionWhenEndDateIsNull() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant now = Instant.parse("2024-06-01T12:00:00Z");

        Mockito.when(pnDeliveryPushConfigs.getStartActionImplWithDaoTimestamp()).thenReturn(start);
        Mockito.when(pnDeliveryPushConfigs.getEndActionImplWithDaoTimestamp()).thenReturn(null);

        assertThrows(PnInternalException.class, () -> featureFlagUtils.isActionImplDao(now));
    }
}