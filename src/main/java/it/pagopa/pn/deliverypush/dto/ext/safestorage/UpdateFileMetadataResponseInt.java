package it.pagopa.pn.deliverypush.dto.ext.safestorage;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class UpdateFileMetadataResponseInt {
    private String resultCode;

    private String resultDescription;

    private List<String> errorList = null;
}
