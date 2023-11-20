package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class PaperSendMode implements Comparable<PaperSendMode>{
    private Instant startConfigurationTime;
    private SendAttachmentMode analogSendAttachmentMode;
    private SendAttachmentMode simpleRegisteredLetterSendAttachmentMode;
    private DocumentComposition.TemplateType aarTemplateType;

    @Override
    public int compareTo(@NotNull PaperSendMode o) {
        return startConfigurationTime.compareTo(o.getStartConfigurationTime());
    }
}
