package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class PaperSendModeUtils {
    public static final String SEPARATOR = ";";
    public static final int INDEX_START_DATE = 0;
    public static final int ANALOG_SEND_ATTACHMENT_MODE_INDEX = 1;
    public static final int SIMPLE_REGISTERED_LETTER_SEND_ATTACHMENT_MODE_INDEX = 2;
    public static final int AAR_TEMPLATE_TYPE_INDEX = 3;
    
    private final List<PaperSendMode> paperSendModesList;
    
    public PaperSendModeUtils(PnDeliveryPushConfigs pnDeliveryPushConfigs){
        List<PaperSendMode> paperSendModesListNotSorted = getPaperSendModeFromString(pnDeliveryPushConfigs.getPaperSendMode());
        paperSendModesList = getSortedList(paperSendModesListNotSorted);
    }
    
    public PaperSendMode getPaperSendMode(Instant time){
        log.debug("Start getPaperSendMode for time={}", time);
        PaperSendMode paperSendMode =  getCorrectPaperSendModeFromDate(time, paperSendModesList);
        log.debug("End getPaperSendMode. PaperSendMode for time={} is {}", time, paperSendMode);
        return paperSendMode;
    }

    private PaperSendMode getCorrectPaperSendModeFromDate(Instant time, List<PaperSendMode> paperSendModesList) {
        for(int i = paperSendModesList.size() - 1; i >=0; i--){
            PaperSendMode elem = paperSendModesList.get(i);
            if( time.isAfter(elem.getStartConfigurationTime()) || time.equals(elem.getStartConfigurationTime()) ){
                return elem;
            }
        }
        return null;
    }

    @NotNull
    private static List<PaperSendMode> getSortedList(List<PaperSendMode> paperSendModesList) {
        List<PaperSendMode> paperSendModesListSorted = new ArrayList<>(paperSendModesList);
        Collections.sort(paperSendModesListSorted);
        log.debug("PaperSendModesListSorted is {}", paperSendModesListSorted);
        return paperSendModesListSorted;
    }

    private List<PaperSendMode> getPaperSendModeFromString(List<String> paperSendModeStringList) {

        return paperSendModeStringList.stream().map( elem -> {
            String[] arrayObj = elem.split(SEPARATOR);
            String configStartDate = arrayObj[INDEX_START_DATE];
            String analogSendAttachmentMode = arrayObj[ANALOG_SEND_ATTACHMENT_MODE_INDEX];
            String simpleRegisteredLetterSendAttachmentMode = arrayObj[SIMPLE_REGISTERED_LETTER_SEND_ATTACHMENT_MODE_INDEX];
            String aarTemplateType = arrayObj[AAR_TEMPLATE_TYPE_INDEX];
            
            return PaperSendMode.builder()
                    .startConfigurationTime(Instant.parse(configStartDate))
                    .analogSendAttachmentMode(SendAttachmentMode.fromValue(analogSendAttachmentMode))
                    .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.fromValue(simpleRegisteredLetterSendAttachmentMode))
                    .aarTemplateType(DocumentComposition.TemplateType.valueOf(aarTemplateType))
                    .build();
        }).toList();
        
    }

}
