package it.pagopa.pn.deliverypush.dto.documentcreation;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class DocumentCreationRequest {
    private String key;
    private String iun;
    private Integer recIndex;
    private DocumentCreationTypeInt documentCreationType;
    private String timelineId;
}
