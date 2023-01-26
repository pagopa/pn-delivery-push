package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import lombok.*;

import javax.annotation.Nullable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class DocumentCreationRequestDetailsInt implements TimelineElementDetailsInt{
    @Nullable
    private Integer recIndex;
    private DocumentCreationTypeInt documentCreationType;
    
    @Override
    public String toLog() {
        if(recIndex != null){
            return String.format(
                    "recIndex=%d_documentCreationType=%s",
                    recIndex,
                    documentCreationType
            );
        }else {
            return String.format(
                    "documentCreationType=%s",
                    documentCreationType
            );
        }
    }
}
