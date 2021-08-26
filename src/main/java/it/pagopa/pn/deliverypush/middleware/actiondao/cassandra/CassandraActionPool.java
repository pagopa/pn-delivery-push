package it.pagopa.pn.deliverypush.middleware.actiondao.cassandra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.ActionDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CassandraActionPool implements ActionDao {

    private final CassandraOperations cassandra;
    private final ObjectWriter actionWriter;
    private final ObjectReader actionReader;

    public CassandraActionPool(CassandraOperations cassandra, ObjectMapper objMapper) {
        this.cassandra = cassandra;
        this.actionWriter = objMapper.writerFor( Action.class );
        this.actionReader = objMapper.readerFor( Action.class );
    }

    @Override
    public void addAction(Action action, String timeSlot) {
        cassandra.insert( dtoToActionEntity( action ) );
        cassandra.insert( dtoToFutureActionEntity( action, timeSlot ) );
    }

    @Override
    public Optional<Action> getActionById(String actionId) {
        ActionEntity entity = cassandra.selectOneById( actionId, ActionEntity.class );
        return Optional.ofNullable( entity )
                .map( en -> fromJson( en.getActionJson() ) );
    }

    @Override
    public List<Action> findActionsByTimeSlot(String timeSlot) {
        return cassandra.select(queryByTimeSlot(timeSlot), FutureActionEntity.class)
                .stream()
                .map( entity -> fromJson( entity.getActionJson() ))
                .collect(Collectors.toList());
    }

    @Override
    public void unSchedule( Action action, String timeSlot ) {
        FutureActionEntity entity = dtoToFutureActionEntity( action, timeSlot );
        cassandra.deleteById( entity.getId(), FutureActionEntity.class );
    }


    private ActionEntity dtoToActionEntity(Action action) {
        return ActionEntity.builder()
                .actionId( action.getActionId() )
                .actionJson( toJson( action ))
                .build();
    }

    private FutureActionEntity dtoToFutureActionEntity(Action dto, String timeSlot ) {
        return FutureActionEntity.builder()
                .id( FutureActionEntityId.builder()
                        .timeSlot( timeSlot )
                        .iun( dto.getIun() )
                        .actionId( dto.getActionId() )
                        .build()
                )
                .notBefore( dto.getNotBefore() )
                .actionJson( toJson( dto ))
                .build();
    }

    private String toJson( Action dto ) {
        try {
            return actionWriter.writeValueAsString( dto );
        } catch (JsonProcessingException exc) {
            throw new PnInternalException( "writing action to json " + dto, exc );
        }
    }

    private Action fromJson( String actionJson ) {
        try {
            return actionReader.readValue( actionJson );
        } catch (JsonProcessingException exc) {
            throw new PnInternalException( "writing action to json " + actionJson, exc );
        }
    }

    private Query queryByTimeSlot( String timeSlot ) {
        return Query.query(Criteria.where("time_slot").is( timeSlot ));
    }

}
