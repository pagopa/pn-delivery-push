package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FeatureEnabledUtilsTest {

    private PnDeliveryPushConfigs configs;
    private FeatureEnabledUtils utils;

    @BeforeEach
    void setUp() {
        configs = Mockito.mock(PnDeliveryPushConfigs.class);
        utils = new FeatureEnabledUtils(configs);
    }

    @Test
    void testIsPfNewWorkflowEnabled_True() {
        Instant start = Instant.parse("2024-06-01T00:00:00Z");
        Instant stop = Instant.parse("2024-06-30T00:00:00Z");
        Instant sentAt = Instant.parse("2024-06-15T00:00:00Z");
        Mockito.when(configs.getPfNewWorkflowStart()).thenReturn(start.toString());
        Mockito.when(configs.getPfNewWorkflowStop()).thenReturn(stop.toString());

        assertTrue(utils.isPfNewWorkflowEnabled(sentAt));
    }

    @Test
    void testIsPfNewWorkflowEnabled_False() {
        Instant start = Instant.parse("2024-06-01T00:00:00Z");
        Instant stop = Instant.parse("2024-06-30T00:00:00Z");
        Instant sentAt = Instant.parse("2024-07-01T00:00:00Z");
        Mockito.when(configs.getPfNewWorkflowStart()).thenReturn(start.toString());
        Mockito.when(configs.getPfNewWorkflowStop()).thenReturn(stop.toString());

        assertFalse(utils.isPfNewWorkflowEnabled(sentAt));
    }

    @Test
    void testIsFeatureAAROnlyPECForRADDAndPFEnabled_True() {
        Mockito.when(configs.getAAROnlyPECForRADDAndPF()).thenReturn("true");
        assertTrue(utils.isFeatureAAROnlyPECForRADDAndPFEnabled());
    }

    @Test
    void testIsFeatureAAROnlyPECForRADDAndPFEnabled_False() {
        Mockito.when(configs.getAAROnlyPECForRADDAndPF()).thenReturn("false");
        assertFalse(utils.isFeatureAAROnlyPECForRADDAndPFEnabled());
    }

    @Test
    void testIsFeatureAAROnlyPECForRADDAndPFEnabled_Null() {
        Mockito.when(configs.getAAROnlyPECForRADDAndPF()).thenReturn(null);
        assertFalse(utils.isFeatureAAROnlyPECForRADDAndPFEnabled());
    }

    @Test
    void testIsSendCourtesyAtAARGenerationEnabled_StartDateNull() {
        Instant sentAt = Instant.now();
        Mockito.when(configs.getSendCourtesyAtChooseDeliveryActivationDate()).thenReturn(null);

        assertFalse(utils.isSendCourtesyAtAARGenerationEnabled(sentAt));
    }

    @Test
    void testIsSendCourtesyAtAARGenerationEnabled_True() {
        Instant start = Instant.parse("2024-06-10T10:00:00Z");
        Instant sentAt = Instant.parse("2024-06-09T10:00:00Z");
        Mockito.when(configs.getSendCourtesyAtChooseDeliveryActivationDate()).thenReturn(start);

        assertTrue(utils.isSendCourtesyAtAARGenerationEnabled(sentAt));
    }

    @Test
    void testIsSendCourtesyAtAARGenerationEnabled_False() {
        Instant start = Instant.parse("2024-06-10T10:00:00Z");
        Instant sentAt = Instant.parse("2024-06-10T10:00:00Z");
        Mockito.when(configs.getSendCourtesyAtChooseDeliveryActivationDate()).thenReturn(start);

        assertFalse(utils.isSendCourtesyAtAARGenerationEnabled(sentAt));
    }

    @Test
    void testIsSendCourtesyAtChooseDeliveryActivationEnabled_StartDateNull() {
        Instant sentAt = Instant.now();
        Mockito.when(configs.getSendCourtesyAtChooseDeliveryActivationDate()).thenReturn(null);

        assertFalse(utils.isSendCourtesyAtChooseDeliveryEnabled(sentAt));
    }

    @Test
    void testIsSendCourtesyAtChooseDeliveryActivationEnabled_True() {
        Instant start = Instant.parse("2024-06-10T10:00:00Z");
        Instant sentAt = Instant.parse("2024-06-10T10:00:00Z");
        Mockito.when(configs.getSendCourtesyAtChooseDeliveryActivationDate()).thenReturn(start);

        assertTrue(utils.isSendCourtesyAtChooseDeliveryEnabled(sentAt));
    }

    @Test
    void testIsSendCourtesyAtChooseDeliveryActivationEnabled_False() {
        Instant start = Instant.parse("2024-06-10T10:00:00Z");
        Instant sentAt = Instant.parse("2024-06-09T10:00:00Z");
        Mockito.when(configs.getSendCourtesyAtChooseDeliveryActivationDate()).thenReturn(start);

        assertFalse(utils.isSendCourtesyAtChooseDeliveryEnabled(sentAt));
    }

}
