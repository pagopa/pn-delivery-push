package it.pagopa.pn.deliverypush.actions;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;

import it.pagopa.pn.commons.abstractions.FileStorage;

class LegalFactUtilsTest {
    private LegalFactUtils legalFactUtils;
    private FileStorage fileStorage;
    private LegalFactPdfGeneratorUtils pdfUtils;
    
    @BeforeEach
    public void setup() {
        fileStorage = Mockito.mock(FileStorage.class);
        pdfUtils = Mockito.mock(LegalFactPdfGeneratorUtils.class);
        legalFactUtils = new LegalFactUtils(
                fileStorage,
                pdfUtils);
    }

    @Test
    void successConversionInstantToDate() {
        Instant testDate = Instant.parse("2021-09-03T13:03:00.000Z");
        String date = legalFactUtils.instantToDate(testDate);
        Assertions.assertEquals("2021-09-03 13:03", date);
    }
    
    @Test
    void successSaveLegalFact() throws IOException {
        //Given
        String iun = "TestIun1";
        String legalFactName = "TestLegalFact";
        byte[] legalFact = new byte[] { 77, 97, 114, 121 };
        Long expectedBodyLength = (long) legalFact.length;
        
        //When
        legalFactUtils.saveLegalFact(iun, legalFactName, legalFact);

        //Then
        ArgumentCaptor<String> keyCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<InputStream> bodyCapture = ArgumentCaptor.forClass(InputStream.class);
        ArgumentCaptor<Long> bodyLengthCapture = ArgumentCaptor.forClass(long.class);
        ArgumentCaptor<Map> mapCapture = ArgumentCaptor.forClass(Map.class);

        Mockito.verify(fileStorage).putFileVersion(
                keyCapture.capture(),
                bodyCapture.capture(),
                bodyLengthCapture.capture(),
                mapCapture.capture()
        );
        
        Assertions.assertTrue(StringUtils.isNotBlank(keyCapture.getValue()));
           
        byte[] body = readByte(bodyCapture.getValue());
        Assertions.assertArrayEquals(legalFact, body, "Different body from the expected");
  
        Assertions.assertEquals(expectedBodyLength, bodyLengthCapture.getValue(), "Different body length from expected");
        Assertions.assertEquals(Collections.singletonMap("Content-Type", "application/pdf; charset=utf-8"), mapCapture.getValue());
    }
    
    @Test
    void onceWriterTest() throws JsonProcessingException {
        //Given
        String iun1 = "Test_iun1";
        String iun2 = "Test_iun2";
        String legalFactName = "TestLegalFact";
        
        byte[] legalFact1 = new byte[] { 77, 97, 114, 121 };
        byte[] legalFact2 = new byte[] { 77, 97, 114, 122 };

        //When
        legalFactUtils.saveLegalFact(iun1, legalFactName, legalFact1);
        legalFactUtils.saveLegalFact(iun2, legalFactName, legalFact2);

        //Then
        Mockito.verify(fileStorage, Mockito.times(2)).putFileVersion(Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyLong(), Mockito.anyMap());
    }

    public byte[] readByte(InputStream inputStream) throws IOException {
        byte[] array = new byte[inputStream.available()];
        inputStream.read(array);

        return array;
   }
        
}
