package it.pagopa.pn.deliverypush.middleware.actiondao.cassandra;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("actions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@ToString
public class ActionEntity {

    @Id()
    private String actionId;

    private String actionJson;

}
