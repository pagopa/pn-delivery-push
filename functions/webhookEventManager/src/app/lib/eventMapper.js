const { parseKinesisObjToJsonObj } = require("./utils");
const crypto = require('crypto');

const TABLES = {
  NOTIFICATIONS: "pn-Notifications",
  TIMELINES: "pn-Timelines",
};



exports.mapEvents = async (events) => {
  const filteredEvents = events.filter((e) => {
    return ( e.eventName == "INSERT" && e.tableName == TABLES.TIMELINES );
  });

  let result = [];

  for (let i = 0; i < filteredEvents.length; i++) {

    let timelineObj = parseKinesisObjToJsonObj(filteredEvents[i].dynamodb.NewImage);

    let date = new Date();

    let action = {
      iun: timelineObj.iun,
      paId: timelineObj.paId,
      timelineId: timelineObj.timelineElementId, 
      eventId: `${date.toISOString()}_${timelineObj.timelineElementId}`,
      type: 'REGISTER_EVENT'
    };

    let messageAttributes = {
      publisher: {
        DataType: 'String',
        StringValue: 'deliveryPush'
      },
      iun: {
        DataType: 'String',
        StringValue: action.iun
      },
      eventId: {
        DataType: 'String',
        StringValue: crypto.randomUUID()
      },
       
      createdAt: {
        DataType: 'String',
        StringValue: date.toISOString()
      }, 
      eventType:  {
        DataType: 'String',
        StringValue:'WEBHOOK_ACTION_GENERIC'
      },
    };
    
    /*
    let webhookEvent = {
      header: header,
      payload: action
    };
    */

    let resultElement = {
      Id: filteredEvents[i].kinesisSeqNumber,
      MessageAttributes: messageAttributes,
      MessageBody: JSON.stringify(action)
    };

    result.push(resultElement);

  }
  return result;
};


