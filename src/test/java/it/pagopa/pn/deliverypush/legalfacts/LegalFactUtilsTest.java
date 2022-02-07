package it.pagopa.pn.deliverypush.legalfacts;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import it.pagopa.pn.commons_delivery.utils.LegalfactsMetadataUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;

import it.pagopa.pn.commons.abstractions.FileStorage;

class LegalFactUtilsTest {
    private LegalFactDao legalFactDao;
    private FileStorage fileStorage;
    private LegalFactGenerator pdfUtils;
    
    private Map<String, String> metadata = new HashMap<>();
    
    @BeforeEach
    public void setup() {
        fileStorage = Mockito.mock(FileStorage.class);
        pdfUtils = Mockito.mock(LegalFactGenerator.class);
        legalFactDao = new LegalFactDao(
                fileStorage,
                pdfUtils,
                new LegalfactsMetadataUtils() );
        metadata.put("Content-Type", "application/pdf; charset=utf-8");
    }
    
    @Test
    void successSaveLegalFact() throws IOException {
        //Given
        String iun = "TestIun1";
        String legalFactName = "TestLegalFact";
        byte[] legalFact = new byte[] { 77, 97, 114, 121 };
        Long expectedBodyLength = (long) legalFact.length;
		
        //When
        legalFactDao.saveLegalFact(iun, legalFactName, legalFact, metadata);

        //Then
        ArgumentCaptor<String> keyCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<InputStream> bodyCapture = ArgumentCaptor.forClass(InputStream.class);
        ArgumentCaptor<Long> bodyLengthCapture = ArgumentCaptor.forClass(long.class);
        ArgumentCaptor<Map> mapCapture = ArgumentCaptor.forClass(Map.class);

        Mockito.verify(fileStorage).putFileVersion(
                keyCapture.capture(),
                bodyCapture.capture(),
                bodyLengthCapture.capture(),
                Mockito.anyString(),
                mapCapture.capture()
        );
        
        Assertions.assertTrue(StringUtils.isNotBlank(keyCapture.getValue()));
           
        byte[] body = readByte(bodyCapture.getValue());
        Assertions.assertArrayEquals(legalFact, body, "Different body from the expected");
  
        Assertions.assertEquals(expectedBodyLength, bodyLengthCapture.getValue(), "Different body length from expected");
        Assertions.assertEquals(Collections.singletonMap("Content-Type", "application/pdf; charset=utf-8"), mapCapture.getValue());
    }
    
    @Test
    void onceWriterTest() throws IOException {
        //Given
        String iun1 = "Test_iun1";
        String iun2 = "Test_iun2";
        String legalFactName = "TestLegalFact";
        
        byte[] legalFact1 = new byte[] { 77, 97, 114, 121 };
        byte[] legalFact2 = new byte[] { 77, 97, 114, 122 };

        //When
        legalFactDao.saveLegalFact(iun1, legalFactName, legalFact1, metadata);
        legalFactDao.saveLegalFact(iun2, legalFactName, legalFact2, metadata);

        //Then
        Mockito.verify(fileStorage, Mockito.times(2)).putFileVersion(
                Mockito.anyString(),
                Mockito.any(InputStream.class),
                Mockito.anyLong(),
                Mockito.anyString(),
                Mockito.anyMap()
            );
    }

    public byte[] readByte(InputStream inputStream) throws IOException {
        byte[] array = new byte[inputStream.available()];
        inputStream.read(array);

        return array;
   }
        
}
