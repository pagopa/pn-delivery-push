package it.pagopa.pn.deliverypush.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.legalfacts.NotificationReceivedLegalFact;
import it.pagopa.pn.api.dto.legalfacts.RecipientInfoWithAddresses;
import it.pagopa.pn.api.dto.legalfacts.SenderInfo;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.deliverypush.actions.LegalFactUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegalFactUtilsTest {
    private LegalFactUtils legalFactUtils;
    private FileStorage fileStorage;
    private ObjectMapper objMapper;
    private ObjectMapper testObjectMapper;

    @BeforeEach
    public void setup() {
        fileStorage = Mockito.mock(FileStorage.class);
        objMapper = Mockito.spy(new ObjectMapper());
        testObjectMapper = new ObjectMapper();
        legalFactUtils = new LegalFactUtils(
                fileStorage,
                objMapper);
    }

    @Test
    void successConversionInstantToDate() {
        Instant testDate = Instant.parse("2021-09-03T13:03:00.000Z");
        String date = legalFactUtils.instantToDate(testDate);
        assertEquals("2021-09-03 13:03", date);
    }

    @Test
    void successSaveLegalFact() throws JsonProcessingException {
        //Given
        String iun = "TestIun1";
        String legalFactName = "TestLegalFact";
        String legalFactJson = "{\"data\":\" \"}";
        Long expectedBodyLength = (long) legalFactJson.length();
        Object legalFact = testObjectMapper.readValue(legalFactJson, Map.class);

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
        Assertions.assertEquals(legalFactJson, readString(bodyCapture.getValue()), "Different body from expected json");
        Assertions.assertEquals(expectedBodyLength, bodyLengthCapture.getValue(), "Different body length from expected");
        Assertions.assertEquals(Collections.singletonMap("Content-Type", "application/json; charset=utf-8"), mapCapture.getValue());

    }

    @Test
    void onceWriterTest() throws JsonProcessingException {
        //Given
        String iun1 = "Test_iun1";
        String iun2 = "Test_iun2";
        String legalFactName = "TestLegalFact";
        String legalFactJson1 = "{\"data\":\"1 \"}";
        String legalFactJson2 = "{\"data\":\"2 \"}";
        Object legalFact1 = testObjectMapper.readValue(legalFactJson1, Map.class);
        Object legalFact2 = testObjectMapper.readValue(legalFactJson2, Map.class);

        //When
        legalFactUtils.saveLegalFact(iun1, legalFactName, legalFact1);
        legalFactUtils.saveLegalFact(iun2, legalFactName, legalFact2);

        //Then
        Mockito.verify(fileStorage, Mockito.times(2)).putFileVersion(Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyLong(), Mockito.anyMap());
        Mockito.verify(objMapper, Mockito.times(1)).writerFor(Mockito.any(Class.class));
    }

    private NotificationReceivedLegalFact newNotificationReceivedLegalFact(Notification notification) {
        return NotificationReceivedLegalFact.builder()
                .iun("IUN_01")
                .date("2021-09-03")
                .digests(notification.getDocuments()
                        .stream()
                        .map(d -> d.getDigests().getSha256())
                        .collect(Collectors.toList()))
                .sender(SenderInfo.builder()
                        .paDenomination("pa_02")
                        .paTaxId("pa_tax_id")
                        .build())
                .recipient(RecipientInfoWithAddresses.builder()
                        .denomination("Nome Cognome/Ragione Sociale")
                        .taxId("Codice Fiscale 01")
                        .digitalDomicile( DigitalAddress.builder()
                                .type( DigitalAddressType.PEC )
                                .address("account@dominio.it")
                                .build()
                        )
                        .physicalDomicile("Via Roma 23")
                        .build())
                .build();
    }

    private String readString(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream))
                .lines().collect(Collectors.joining("\n"));
    }
}
