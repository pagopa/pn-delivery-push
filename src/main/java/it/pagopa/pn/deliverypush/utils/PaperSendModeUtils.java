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
    
    private final List<PnSendMode> pnSendModesList;
    
    public PaperSendModeUtils(PnDeliveryPushConfigs pnDeliveryPushConfigs){
        List<PnSendMode> pnSendModesListNotSorted = getPaperSendModeFromString(pnDeliveryPushConfigs.getPaperSendMode());
        pnSendModesList = getSortedList(pnSendModesListNotSorted);
    }
    
    public PnSendMode getPaperSendMode(Instant time){
        log.debug("Start getPaperSendMode for time={}", time);
        PnSendMode pnSendMode =  getCorrectPaperSendModeFromDate(time, pnSendModesList);
        log.debug("End getPaperSendMode. PaperSendMode for time={} is {}", time, pnSendMode);
        return pnSendMode;
    }

    private PnSendMode getCorrectPaperSendModeFromDate(Instant time, List<PnSendMode> pnSendModesList) {
        for(int i = pnSendModesList.size() - 1; i >=0; i--){
            PnSendMode elem = pnSendModesList.get(i);
            if( time.isAfter(elem.getStartConfigurationTime()) || time.equals(elem.getStartConfigurationTime()) ){
                return elem;
            }
        }
        return null;
    }

    @NotNull
    private static List<PnSendMode> getSortedList(List<PnSendMode> pnSendModesList) {
        List<PnSendMode> pnSendModesListSorted = new ArrayList<>(pnSendModesList);
        Collections.sort(pnSendModesListSorted);
        log.debug("PaperSendModesListSorted is {}", pnSendModesListSorted);
        return pnSendModesListSorted;
    }

    private List<PnSendMode> getPaperSendModeFromString(List<String> paperSendModeStringList) {

        return paperSendModeStringList.stream().map( elem -> {
            String[] arrayObj = elem.split(SEPARATOR);
            String configStartDate = arrayObj[INDEX_START_DATE];
            String analogSendAttachmentMode = arrayObj[ANALOG_SEND_ATTACHMENT_MODE_INDEX];
            String simpleRegisteredLetterSendAttachmentMode = arrayObj[SIMPLE_REGISTERED_LETTER_SEND_ATTACHMENT_MODE_INDEX];
            String aarTemplateType = arrayObj[AAR_TEMPLATE_TYPE_INDEX];
            
            return PnSendMode.builder()
                    .startConfigurationTime(Instant.parse(configStartDate))
                    .analogSendAttachmentMode(SendAttachmentMode.fromValue(analogSendAttachmentMode))
                    .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.fromValue(simpleRegisteredLetterSendAttachmentMode))
                    .aarTemplateType(DocumentComposition.TemplateType.valueOf(aarTemplateType))
                    .build();
        }).toList();
        
    }

}
