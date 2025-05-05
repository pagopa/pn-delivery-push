package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.cost.RefusalCostMode;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

class RefusalCostCalculatorTest {
    @Mock
    private PnTechnicalRefusalCostMode pnTechnicalRefusalCostMode;
    @Mock
    private NotificationProcessCostService notificationProcessCostService;
    @Mock
    private RefusalCostCalculator refusalCostCalculator;
    
    private static final int SEND_FEE = 100;

    private static final String TECHNICAL_ERROR_CODE = "ADDRESS_SEARCH_FAILED";

    @BeforeEach
    void setUp() {
        pnTechnicalRefusalCostMode = mock(PnTechnicalRefusalCostMode.class);
        notificationProcessCostService = mock(NotificationProcessCostService.class);
        when(notificationProcessCostService.getSendFee()).thenReturn(SEND_FEE);
        refusalCostCalculator = new RefusalCostCalculator(pnTechnicalRefusalCostMode, notificationProcessCostService);
    }

    @Test
    void calculateRefusalCost_NoTechnicalErrors() {
        NotificationInt notification = mock(NotificationInt.class);
        when(notification.getRecipients()).thenReturn(getRecipientsList());

        int result = refusalCostCalculator.calculateRefusalCost(notification, Collections.emptyList());

        assertEquals(3 * SEND_FEE, result);
        verify(notificationProcessCostService, times(1)).getSendFee();
    }

    @Test
    void calculateRefusalCost_WithTechnicalErrors_UniformMode() {
        NotificationInt notification = mock(NotificationInt.class);
        when(notification.getRecipients()).thenReturn(getRecipientsList());
        when(pnTechnicalRefusalCostMode.getMode()).thenReturn(RefusalCostMode.UNIFORM);
        when(pnTechnicalRefusalCostMode.getCost()).thenReturn(50);

        int result = refusalCostCalculator.calculateRefusalCost(notification, getErrorsList());

        assertEquals(50, result);
        verify(pnTechnicalRefusalCostMode, times(1)).getMode();
        verify(pnTechnicalRefusalCostMode, times(1)).getCost();
    }

    @Test
    public void calculateRefusalCost_WithTechnicalErrors_RecipientBasedMode() {
        NotificationInt notification = mock(NotificationInt.class);
        when(notification.getRecipients()).thenReturn(getRecipientsList());
        when(pnTechnicalRefusalCostMode.getMode()).thenReturn(RefusalCostMode.RECIPIENT_BASED);
        when(pnTechnicalRefusalCostMode.getCost()).thenReturn(20);

        int result = refusalCostCalculator.calculateRefusalCost(notification, getErrorsList());

        assertEquals(220, result); // 1 recipient with technical error (20) + 2 recipients without (100 each)
        verify(pnTechnicalRefusalCostMode, times(1)).getMode();
        verify(pnTechnicalRefusalCostMode, times(1)).getCost();
        verify(notificationProcessCostService, times(1)).getSendFee();
    }

    @Test
    void calculateRefusalCost_InvalidNumberOfRecipients() {
        NotificationInt notification = mock(NotificationInt.class);
        when(notification.getRecipients()).thenReturn(getRecipientsList()); // 3 recipients
        when(pnTechnicalRefusalCostMode.getMode()).thenReturn(RefusalCostMode.RECIPIENT_BASED);
        when(pnTechnicalRefusalCostMode.getCost()).thenReturn(20);

        List<NotificationRefusedErrorInt> errors = new ArrayList<>();
        errors.add(NotificationRefusedErrorInt.builder().recIndex(0).errorCode(TECHNICAL_ERROR_CODE).build());
        errors.add(NotificationRefusedErrorInt.builder().recIndex(1).errorCode(TECHNICAL_ERROR_CODE).build());
        errors.add(NotificationRefusedErrorInt.builder().recIndex(2).errorCode(TECHNICAL_ERROR_CODE).build());
        errors.add(NotificationRefusedErrorInt.builder().recIndex(3).errorCode(TECHNICAL_ERROR_CODE).build()); // Extra error

        PnInternalException exception = assertThrows(PnInternalException.class, () ->
                refusalCostCalculator.calculateRefusalCost(notification, errors)
        );

        assertEquals("Invalid number of recipients not affected by technical errors", exception.getProblem().getDetail());
        assertEquals("INVALID_NUMBER_OF_RECIPIENTS", exception.getProblem().getErrors().get(0).getCode());
    }

    private List<NotificationRecipientInt> getRecipientsList() {
        List<NotificationRecipientInt> recipients = new ArrayList<>();

        recipients.add(NotificationRecipientInt.builder()
                .taxId("taxId1")
                .recipientType(RecipientTypeInt.PF)
                .build());

        recipients.add(NotificationRecipientInt.builder()
                .taxId("taxId2")
                .recipientType(RecipientTypeInt.PG)
                .build());

        recipients.add(NotificationRecipientInt.builder()
                .taxId("taxId3")
                .recipientType(RecipientTypeInt.PG)
                .build());

        return recipients;
    }

    private List<NotificationRefusedErrorInt> getErrorsList() {
        List<NotificationRefusedErrorInt> errors = new ArrayList<>();
        errors.add(NotificationRefusedErrorInt.builder()
                .recIndex(0)
                .errorCode(TECHNICAL_ERROR_CODE)
                .build());
        errors.add(NotificationRefusedErrorInt.builder()
                .recIndex(1)
                .errorCode("ERROR_CODE")
                .build());
        errors.add(NotificationRefusedErrorInt.builder()
                .recIndex(2)
                .errorCode("ERROR_CODE")
                .build());
        return errors;
    }
}