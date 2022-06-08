package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.deliverypush.externalclient.pnclient.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.externalclient.pnclient.safestorage.PnSafeStorageClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;


class LegalFactUtilsTest {
    private LegalFactDao legalFactDao;
    private LegalFactGenerator pdfUtils;
    private PnSafeStorageClient safeStorageClient;

    
    @BeforeEach
    public void setup() {
        pdfUtils = Mockito.mock(LegalFactGenerator.class);
        safeStorageClient = Mockito.mock(PnSafeStorageClient.class);
        legalFactDao = new LegalFactDao(
                pdfUtils,
                safeStorageClient
                );
    }
    
    @Test
    void successSaveLegalFact() {
        //Given
        String iun = "TestIun1";
        String legalFactName = "TestLegalFact";
        byte[] legalFact = new byte[] { 77, 97, 114, 121 };
        int expectedBodyLength = legalFact.length;

        FileCreationResponse response = new FileCreationResponse();
        response.setKey("123");
        response.setSecret("abc");
        response.setUploadUrl("https://www.unqualcheurl.it");
        response.setUploadMethod(FileCreationResponse.UploadMethodEnum.POST);

        when(safeStorageClient.createAndUploadContent(Mockito.any())).thenReturn(response);

        //When
        legalFactDao.saveLegalFact(legalFact);

        //Then
        ArgumentCaptor<FileCreationWithContentRequest> argCapture = ArgumentCaptor.forClass(FileCreationWithContentRequest.class);

        Mockito.verify(safeStorageClient).createAndUploadContent(
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

        FileCreationResponse response = new FileCreationResponse();
        response.setKey("123");
        response.setSecret("abc");
        response.setUploadUrl("https://www.unqualcheurl.it");
        response.setUploadMethod(FileCreationResponse.UploadMethodEnum.POST);

        when(safeStorageClient.createAndUploadContent(Mockito.any())).thenReturn(response);

        //When
        legalFactDao.saveLegalFact(legalFact1);
        legalFactDao.saveLegalFact(legalFact2);

        //Then
        Mockito.verify(safeStorageClient, Mockito.times(2)).createAndUploadContent(
                Mockito.any()
            );
    }
        
}
