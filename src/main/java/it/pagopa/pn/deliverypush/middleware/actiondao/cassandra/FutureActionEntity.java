package it.pagopa.pn.deliverypush.middleware.actiondao.cassandra;

import lombok.*;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Table("future_actions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@ToString
public class FutureActionEntity {

    @PrimaryKey
    private FutureActionEntityId id;

    private Instant notBefore;

    private String actionJson;

}
