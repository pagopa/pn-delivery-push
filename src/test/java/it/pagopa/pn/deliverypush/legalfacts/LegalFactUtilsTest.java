package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.impl.SafeStorageServiceImpl;
import it.pagopa.pn.deliverypush.service.impl.SaveLegalFactsServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;


class LegalFactUtilsTest {
    private SaveLegalFactsServiceImpl legalFactsService;
    private LegalFactGenerator pdfUtils;
    private SafeStorageService safeStorageService;

    
    @BeforeEach
    public void setup() {
        pdfUtils = Mockito.mock(LegalFactGenerator.class);
        safeStorageService = Mockito.mock(SafeStorageServiceImpl.class);
        legalFactsService = new SaveLegalFactsServiceImpl(
                pdfUtils,
                safeStorageService
                );
    }
    
    @Test
    void successSaveLegalFact() {
        //Given
        String iun = "TestIun1";
        String legalFactName = "TestLegalFact";
        byte[] legalFact = new byte[] { 77, 97, 114, 121 };
        int expectedBodyLength = legalFact.length;

        FileCreationResponseInt response = new FileCreationResponseInt();
        response.setKey("123");

        when(safeStorageService.createAndUploadContent(Mockito.any())).thenReturn(response);

        //When
        legalFactsService.saveLegalFact(legalFact);

        //Then
        ArgumentCaptor<FileCreationWithContentRequest> argCapture = ArgumentCaptor.forClass(FileCreationWithContentRequest.class);

        Mockito.verify(safeStorageService).createAndUploadContent(
                argCapture.capture()
        );
        
        Assertions.assertNotNull(argCapture);
           
        byte[] body = argCapture.getValue().getContent();
        Assertions.assertArrayEquals(legalFact, body, "Different body from the expected");
  
        Assertions.assertEquals(expectedBodyLength, body.length, "Different body length from expected");
        Assertions.assertEquals("application/pdf", argCapture.getValue().getContentType());
    }
    
    @Test
    void onceWriterTest() {
        //Given
        String iun1 = "Test_iun1";
        String iun2 = "Test_iun2";
        String legalFactName = "TestLegalFact";
        
        byte[] legalFact1 = new byte[] { 77, 97, 114, 121 };
        byte[] legalFact2 = new byte[] { 77, 97, 114, 122 };

        FileCreationResponseInt response = new FileCreationResponseInt();
        response.setKey("123");

        when(safeStorageService.createAndUploadContent(Mockito.any())).thenReturn(response);

        //When
        legalFactsService.saveLegalFact(legalFact1);
        legalFactsService.saveLegalFact(legalFact2);

        //Then
        Mockito.verify(safeStorageService, Mockito.times(2)).createAndUploadContent(
                Mockito.any()
            );
    }
        
}
