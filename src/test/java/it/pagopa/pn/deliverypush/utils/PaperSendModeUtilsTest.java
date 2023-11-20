package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.deliverypush.utils.PaperSendModeUtils.*;

@ExtendWith(SpringExtension.class)
class PaperSendModeUtilsTest {
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private PaperSendModeUtils paperSendModeUtils;

    @Test
    void getPaperSendModeNoConfiguration() {
        //GIVEN
        List<String> configurationList = new ArrayList<>();
        String firstCorrectConfiguration = "2022-11-30T23:00:00Z;AAR;AAR;AAR_NOTIFICATION_RADD";
        String[] arrayObj = firstCorrectConfiguration.split(SEPARATOR);
        Instant correctConfigStartDate = Instant.parse(arrayObj[INDEX_START_DATE]);
        configurationList.add(firstCorrectConfiguration);
        Mockito.when(pnDeliveryPushConfigs.getPaperSendMode()).thenReturn(configurationList);
        paperSendModeUtils = new PaperSendModeUtils(pnDeliveryPushConfigs);

        Instant time = correctConfigStartDate.minus(1, ChronoUnit.DAYS);
        //WHEN
        PaperSendMode paperSendMode = paperSendModeUtils.getPaperSendMode(time);
        //THEN
        Assertions.assertNull(paperSendMode);
    }

    @Test
    void getPaperSendModeOneConfigurationOk() {
        //GIVEN
        List<String> configurationList = new ArrayList<>();
        String firstCorrectConfiguration = "2022-11-30T23:00:00Z;AAR;AAR;AAR_NOTIFICATION_RADD";
        String[] arrayObj = firstCorrectConfiguration.split(SEPARATOR);
        Instant correctConfigStartDate = Instant.parse(arrayObj[INDEX_START_DATE]);
        SendAttachmentMode correctAnalogSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[ANALOG_SEND_ATTACHMENT_MODE_INDEX]);
        SendAttachmentMode correctSimpleRegisteredLetterSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[SIMPLE_REGISTERED_LETTER_SEND_ATTACHMENT_MODE_INDEX]);
        DocumentComposition.TemplateType correctAarTemplateType = DocumentComposition.TemplateType.valueOf(arrayObj[AAR_TEMPLATE_TYPE_INDEX]);
        configurationList.add(firstCorrectConfiguration);
        Mockito.when(pnDeliveryPushConfigs.getPaperSendMode()).thenReturn(configurationList);
        paperSendModeUtils = new PaperSendModeUtils(pnDeliveryPushConfigs);
        Instant time = correctConfigStartDate.plus(1, ChronoUnit.DAYS);

        //WHEN
        PaperSendMode paperSendMode = paperSendModeUtils.getPaperSendMode(time);

        //THEN
        Assertions.assertNotNull(paperSendMode);
        Assertions.assertEquals(correctConfigStartDate, paperSendMode.getStartConfigurationTime());
        Assertions.assertEquals(correctAnalogSendAttachmentMode, paperSendMode.getAnalogSendAttachmentMode());
        Assertions.assertEquals(correctSimpleRegisteredLetterSendAttachmentMode, paperSendMode.getSimpleRegisteredLetterSendAttachmentMode());
        Assertions.assertEquals(correctAarTemplateType, paperSendMode.getAarTemplateType());
    }

    @Test
    void getPaperSendModeOneConfigurationMoreItem() {
        //GIVEN
        List<String> configurationList = new ArrayList<>();
        String firstConfiguration = "2022-11-30T23:00:00Z;AAR;AAR;AAR_NOTIFICATION_RADD";
        String secondCorrectConfiguration = "2022-12-20T23:00:00Z;AAR-DOCUMENTS;AAR-DOCUMENTS;AAR_NOTIFICATION_RADD";
        String thirdConfiguration = "2022-12-31T23:00:00Z;AAR-DOCUMENTS-PAYMENTS;AAR;AAR_NOTIFICATION";

        configurationList.add(firstConfiguration);
        configurationList.add(secondCorrectConfiguration);
        configurationList.add(thirdConfiguration);

        String[] arrayObj = secondCorrectConfiguration.split(SEPARATOR);
        Instant correctConfigStartDate = Instant.parse(arrayObj[INDEX_START_DATE]);
        SendAttachmentMode correctAnalogSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[ANALOG_SEND_ATTACHMENT_MODE_INDEX]);
        SendAttachmentMode correctSimpleRegisteredLetterSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[SIMPLE_REGISTERED_LETTER_SEND_ATTACHMENT_MODE_INDEX]);
        DocumentComposition.TemplateType correctAarTemplateType = DocumentComposition.TemplateType.valueOf(arrayObj[AAR_TEMPLATE_TYPE_INDEX]);
        Mockito.when(pnDeliveryPushConfigs.getPaperSendMode()).thenReturn(configurationList);
        paperSendModeUtils = new PaperSendModeUtils(pnDeliveryPushConfigs);

        Instant time = correctConfigStartDate.plus(1, ChronoUnit.DAYS);
        //WHEN
        PaperSendMode paperSendMode = paperSendModeUtils.getPaperSendMode(time);
        //THEN
        Assertions.assertNotNull(paperSendMode);
        Assertions.assertEquals(correctConfigStartDate, paperSendMode.getStartConfigurationTime());
        Assertions.assertEquals(correctAnalogSendAttachmentMode, paperSendMode.getAnalogSendAttachmentMode());
        Assertions.assertEquals(correctSimpleRegisteredLetterSendAttachmentMode, paperSendMode.getSimpleRegisteredLetterSendAttachmentMode());
        Assertions.assertEquals(correctAarTemplateType, paperSendMode.getAarTemplateType());
    }

    @Test
    void getPaperSendModeOneConfigurationSameDate() {
        //GIVEN
        List<String> configurationList = new ArrayList<>();
        String firstConfiguration = "1970-01-01T00:00:00Z;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION";
        String secondCorrectConfiguration = "2023-11-30T23:00:00Z;AAR;AAR;AAR_NOTIFICATION_RADD";

        configurationList.add(firstConfiguration);
        configurationList.add(secondCorrectConfiguration);

        String[] arrayObj = secondCorrectConfiguration.split(SEPARATOR);
        Instant correctConfigStartDate = Instant.parse(arrayObj[INDEX_START_DATE]);
        SendAttachmentMode correctAnalogSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[ANALOG_SEND_ATTACHMENT_MODE_INDEX]);
        SendAttachmentMode correctSimpleRegisteredLetterSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[SIMPLE_REGISTERED_LETTER_SEND_ATTACHMENT_MODE_INDEX]);
        DocumentComposition.TemplateType correctAarTemplateType = DocumentComposition.TemplateType.valueOf(arrayObj[AAR_TEMPLATE_TYPE_INDEX]);

        Mockito.when(pnDeliveryPushConfigs.getPaperSendMode()).thenReturn(configurationList);
        paperSendModeUtils = new PaperSendModeUtils(pnDeliveryPushConfigs);

        Instant time = correctConfigStartDate;
        //WHEN
        PaperSendMode paperSendMode = paperSendModeUtils.getPaperSendMode(time);

        //THEN
        Assertions.assertNotNull(paperSendMode);
        Assertions.assertEquals(correctConfigStartDate, paperSendMode.getStartConfigurationTime());
        Assertions.assertEquals(correctAnalogSendAttachmentMode, paperSendMode.getAnalogSendAttachmentMode());
        Assertions.assertEquals(correctSimpleRegisteredLetterSendAttachmentMode, paperSendMode.getSimpleRegisteredLetterSendAttachmentMode());
        Assertions.assertEquals(correctAarTemplateType, paperSendMode.getAarTemplateType());
    }

    @Test
    void getPaperSendModeOneConfigurationSameDateAfter() {
        //GIVEN
        List<String> configurationList = new ArrayList<>();
        String firstConfiguration = "1970-01-01T00:00:00Z;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION";
        String secondCorrectConfiguration = "2023-11-30T23:00:00Z;AAR;AAR;AAR_NOTIFICATION_RADD";

        configurationList.add(firstConfiguration);
        configurationList.add(secondCorrectConfiguration);

        String[] arrayObj = secondCorrectConfiguration.split(SEPARATOR);
        Instant correctConfigStartDate = Instant.parse(arrayObj[INDEX_START_DATE]);
        SendAttachmentMode correctAnalogSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[ANALOG_SEND_ATTACHMENT_MODE_INDEX]);
        SendAttachmentMode correctSimpleRegisteredLetterSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[SIMPLE_REGISTERED_LETTER_SEND_ATTACHMENT_MODE_INDEX]);
        DocumentComposition.TemplateType correctAarTemplateType = DocumentComposition.TemplateType.valueOf(arrayObj[AAR_TEMPLATE_TYPE_INDEX]);

        Mockito.when(pnDeliveryPushConfigs.getPaperSendMode()).thenReturn(configurationList);
        paperSendModeUtils = new PaperSendModeUtils(pnDeliveryPushConfigs);

        Instant time = correctConfigStartDate.plus(1, ChronoUnit.DAYS);

        //WHEN
        PaperSendMode paperSendMode = paperSendModeUtils.getPaperSendMode(time);

        //THEN
        Assertions.assertNotNull(paperSendMode);
        Assertions.assertEquals(correctConfigStartDate, paperSendMode.getStartConfigurationTime());
        Assertions.assertEquals(correctAnalogSendAttachmentMode, paperSendMode.getAnalogSendAttachmentMode());
        Assertions.assertEquals(correctSimpleRegisteredLetterSendAttachmentMode, paperSendMode.getSimpleRegisteredLetterSendAttachmentMode());
        Assertions.assertEquals(correctAarTemplateType, paperSendMode.getAarTemplateType());
    }

    @Test
    void getPaperSendModeOneConfigurationSameDateBefore() {
        //GIVEN
        List<String> configurationList = new ArrayList<>();
        String firstCorrectConfiguration = "1970-01-01T00:00:00Z;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION";
        String secondConfiguration = "2023-11-30T23:00:00Z;AAR;AAR;AAR_NOTIFICATION_RADD";

        configurationList.add(firstCorrectConfiguration);
        configurationList.add(secondConfiguration);

        String[] arrayObj = firstCorrectConfiguration.split(SEPARATOR);
        Instant correctConfigStartDate = Instant.parse(arrayObj[INDEX_START_DATE]);
        SendAttachmentMode correctAnalogSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[ANALOG_SEND_ATTACHMENT_MODE_INDEX]);
        SendAttachmentMode correctSimpleRegisteredLetterSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[SIMPLE_REGISTERED_LETTER_SEND_ATTACHMENT_MODE_INDEX]);
        DocumentComposition.TemplateType correctAarTemplateType = DocumentComposition.TemplateType.valueOf(arrayObj[AAR_TEMPLATE_TYPE_INDEX]);

        Mockito.when(pnDeliveryPushConfigs.getPaperSendMode()).thenReturn(configurationList);
        paperSendModeUtils = new PaperSendModeUtils(pnDeliveryPushConfigs);

        String[] secondConfObj = secondConfiguration.split(SEPARATOR);
        Instant secondConfStartDate = Instant.parse(secondConfObj[INDEX_START_DATE]);

        Instant time = secondConfStartDate.minus(1, ChronoUnit.SECONDS);
        //WHEN
        PaperSendMode paperSendMode = paperSendModeUtils.getPaperSendMode(time);

        //THEN
        Assertions.assertNotNull(paperSendMode);
        Assertions.assertEquals(correctConfigStartDate, paperSendMode.getStartConfigurationTime());
        Assertions.assertEquals(correctAnalogSendAttachmentMode, paperSendMode.getAnalogSendAttachmentMode());
        Assertions.assertEquals(correctSimpleRegisteredLetterSendAttachmentMode, paperSendMode.getSimpleRegisteredLetterSendAttachmentMode());
        Assertions.assertEquals(correctAarTemplateType, paperSendMode.getAarTemplateType());
    }

    @Test
    void noConfiguration() {
        List<String> configurationList = new ArrayList<>();

        Mockito.when(pnDeliveryPushConfigs.getPaperSendMode()).thenReturn(configurationList);
        paperSendModeUtils = new PaperSendModeUtils(pnDeliveryPushConfigs);

        Instant time = Instant.now();
        PaperSendMode paperSendMode = paperSendModeUtils.getPaperSendMode(time);

        Assertions.assertNull(paperSendMode);
    }

}