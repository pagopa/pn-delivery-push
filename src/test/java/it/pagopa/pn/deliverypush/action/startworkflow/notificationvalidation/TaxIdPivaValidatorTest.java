package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.nationalregistries.CheckTaxIdOKInt;
import it.pagopa.pn.deliverypush.exceptions.PnValidationTaxIdNotValidException;
import it.pagopa.pn.deliverypush.service.NationalRegistriesService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TaxIdPivaValidatorTest {

    @Mock
    private NationalRegistriesService nationalRegistriesService;
    private NotificationUtils notificationUtils;
    
    private TaxIdPivaValidator taxIdPivaValidator;

    @BeforeEach
    public void setup() {
        notificationUtils = new NotificationUtils();
        taxIdPivaValidator = new TaxIdPivaValidator(nationalRegistriesService, notificationUtils);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateTaxIdPivaOk() {
        //Given
        NotificationInt notification = TestUtils.getNotification();

        CheckTaxIdOKInt checkTaxIdOK = CheckTaxIdOKInt.builder()
                .taxId("taxId")
                .isValid(true)
                .build();
        Mockito.when(nationalRegistriesService.checkTaxId(Mockito.anyString())).thenReturn(checkTaxIdOK);

        //when
        Assertions.assertDoesNotThrow( () -> taxIdPivaValidator.validateTaxIdPiva(notification));
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateTaxIdPivaKo() {
        //Given
        NotificationInt notification = TestUtils.getNotification();

        CheckTaxIdOKInt checkTaxIdOK = CheckTaxIdOKInt.builder()
                .taxId("taxId")
                .isValid(false)
                .errorCode("error")
                .build();
        
        Mockito.when(nationalRegistriesService.checkTaxId(Mockito.anyString())).thenReturn(checkTaxIdOK);

        //when
        assertThrows(PnValidationTaxIdNotValidException.class, () -> taxIdPivaValidator.validateTaxIdPiva(notification));
    }
}