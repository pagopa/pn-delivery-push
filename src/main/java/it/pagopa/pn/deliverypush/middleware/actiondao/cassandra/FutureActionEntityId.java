package it.pagopa.pn.deliverypush.middleware.actiondao.cassandra;

import lombok.*;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

@PrimaryKeyClass
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class FutureActionEntityId {

    @PrimaryKeyColumn( name = "time_slot", ordinal = 0, type = PrimaryKeyType.PARTITIONED )
    private String timeSlot;

    @PrimaryKeyColumn( name = "iun", ordinal = 1, type = PrimaryKeyType.CLUSTERED )
    private String iun;

    @PrimaryKeyColumn( name = "action_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED )
    private String actionId;

}
