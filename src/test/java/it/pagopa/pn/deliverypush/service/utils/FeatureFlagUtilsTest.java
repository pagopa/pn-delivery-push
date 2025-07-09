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
        String start = getEpochString(Instant.parse("2024-01-01T00:00:00Z"));
        String end =  getEpochString(Instant.parse("2025-01-01T00:00:00Z"));
        Instant now = Instant.parse("2024-06-01T12:00:00Z");

        Mockito.when(pnDeliveryPushConfigs.getStartActionImplWithDaoEpoch()).thenReturn(start);
        Mockito.when(pnDeliveryPushConfigs.getEndActionImplWithDaoEpoch()).thenReturn(end);

        assertTrue(featureFlagUtils.isActionImplDao(now));
    }

    @Test
    void isActionImplDao_returnsFalseWhenNowBeforeStart() {
        String start = getEpochString(Instant.parse("2024-07-01T00:00:00Z"));
        String end = getEpochString(Instant.parse("2025-01-01T00:00:00Z"));
        Instant now = Instant.parse("2024-06-01T12:00:00Z");

        Mockito.when(pnDeliveryPushConfigs.getStartActionImplWithDaoEpoch()).thenReturn(start);
        Mockito.when(pnDeliveryPushConfigs.getEndActionImplWithDaoEpoch()).thenReturn(end);

        assertFalse(featureFlagUtils.isActionImplDao(now));
    }

    @Test
    void isActionImplDao_returnsFalseWhenNowAfterEnd() {
        String start = getEpochString(Instant.parse("2024-01-01T00:00:00Z"));
        String end = getEpochString(Instant.parse("2024-06-01T00:00:00Z"));
        Instant now = Instant.parse("2024-07-01T12:00:00Z");

        Mockito.when(pnDeliveryPushConfigs.getStartActionImplWithDaoEpoch()).thenReturn(start);
        Mockito.when(pnDeliveryPushConfigs.getEndActionImplWithDaoEpoch()).thenReturn(end);

        assertFalse(featureFlagUtils.isActionImplDao(now));
    }

    @Test
    void isActionImplDao_throwsExceptionWhenStartDateIsNull() {
        String end = getEpochString(Instant.parse("2025-01-01T00:00:00Z"));
        Instant now = Instant.parse("2024-06-01T12:00:00Z");

        Mockito.when(pnDeliveryPushConfigs.getStartActionImplWithDaoEpoch()).thenReturn(null);
        Mockito.when(pnDeliveryPushConfigs.getEndActionImplWithDaoEpoch()).thenReturn(end);

        assertThrows(PnInternalException.class, () -> featureFlagUtils.isActionImplDao(now));
    }

    @Test
    void isActionImplDao_throwsExceptionWhenEndDateIsNull() {
        String start = getEpochString(Instant.parse("2024-01-01T00:00:00Z"));
        Instant now = Instant.parse("2024-06-01T12:00:00Z");

        Mockito.when(pnDeliveryPushConfigs.getStartActionImplWithDaoEpoch()).thenReturn(start);
        Mockito.when(pnDeliveryPushConfigs.getEndActionImplWithDaoEpoch()).thenReturn(null);

        assertThrows(PnInternalException.class, () -> featureFlagUtils.isActionImplDao(now));
    }

    private String getEpochString(Instant instant) {
        return String.valueOf(instant.getEpochSecond());
    }
}