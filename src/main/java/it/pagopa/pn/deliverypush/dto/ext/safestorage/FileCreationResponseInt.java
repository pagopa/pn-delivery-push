package it.pagopa.pn.deliverypush.dto.ext.safestorage;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class FileCreationResponseInt {
    private String key;
}
