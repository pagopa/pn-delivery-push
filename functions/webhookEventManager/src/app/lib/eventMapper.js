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

    let header = {
      publisher: 'deliveryPush',
      iun: action.iun,
      eventId: crypto.randomUUID(), 
      createdAt: date.toISOString(), 
      eventType: 'WEBHOOK_ACTION_GENERIC'
    };
    
    let webhookEvent = {
      header: header,
      payload: action
    };

    result.push(webhookEvent);

  }
  return result;
};


