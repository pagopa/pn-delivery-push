const { SQSClient, SendMessageBatchCommand } = require("@aws-sdk/client-sqs");
const { NodeHttpHandler } = require("@aws-sdk/node-http-handler");
const { v4: uuidv4 } = require("uuid");
const { isTimeToLeave } = require("../utils/utils.js");

const DEFAULT_SOCKET_TIMEOUT = 1000;
const DEFAULT_REQUEST_TIMEOUT = 1000;
const DEFAULT_CONNECTION_TIMEOUT = 1000;

const sqs = new SQSClient({
    requestHandler: new NodeHttpHandler({
      connectionTimeout: DEFAULT_CONNECTION_TIMEOUT,
      requestTimeout: DEFAULT_REQUEST_TIMEOUT,
      socketTimeout: DEFAULT_SOCKET_TIMEOUT,
    }),
  });

let sqsParams = { region: process.env.REGION };
if (process.env.ENDPOINT) 
sqsParams.endpoint = process.env.ENDPOINT;
const QUEUE_URL = process.env.QUEUE_URL;

async function writeMessagesToQueue(immediateActions, context) {
    while (immediateActions.length > 0 && !isTimeToLeave(context)) {
      console.log(
        "Proceeding to send " +
          immediateActions.length +
          " messages to " +
          QUEUE_URL
      );
  
      let splicedActionsArray = immediateActions.splice(0, 1); // prendo i primi 10 e rimuovendoli dall'array originale
  
      let actionsToSendMapped = [];
      splicedActionsArray.forEach(function (action) {
        let messageToSend = mapActionToQueueMessage(action);
        actionsToSendMapped.push(messageToSend);
      });
  
      const input = {
        Entries: actionsToSendMapped,
        QueueUrl: QUEUE_URL,
        //requestHandler: defaultRequestHandler,
      };
  
      console.log("Sending batch message: %j", input);
  
      const command = new SendMessageBatchCommand(input);
      try {
        const response = await sqs.send(command);
        console.log("Sent message response: %j", response);
  
        if (response.Failed && response.Failed.length > 0) {
          splicedActionsArray.forEach((element) => {
            if (
              response.Failed.filter((currFailed) => currFailed.Id == element.Id)
                .length !== 0
            )
              immediateActions.push(element); //Se fallisce nella put, l'action viene reinserita tra quelle da inviare
          });
  
          return immediateActions;
        }
      }
      catch (exceptions) {
        console.log("Error in send sqs message ", exceptions);
        console.log("Stringfy exception ", JSON.stringify(exceptions));
    
        if(exceptions.name && (
            exceptions.name === 'TimeoutError' ||
            exceptions.name === 'RequestTimeout' ||
            exceptions.name === 'RequestTimeoutException')
          ){
          //With timeout error, the record is assumed to be sent by SQS anyway. Need to check with log
          console.error('[SQS_TIMEOUT_ERROR] Timeout record in send sqs, need to check the records ', JSON.stringify(actionsToSendMapped));
        }else{
          console.log('Generic exception in SQS send message, need to reschedule');
          immediateActions = immediateActions.concat(splicedActionsArray);
        }
  
      }
    }
  
    return immediateActions;
}

function mapActionToQueueMessage(action) {
    let uuid = uuidv4();
    let copiedAction = Object.assign({}, action);
    delete copiedAction.kinesisSeqNo;
  
    const message = {
      Id: uuid,
      DelaySeconds: 0,
      MessageAttributes: {
        createdAt: {
          DataType: "String",
          StringValue: new Date().toISOString(),
        },
        eventId: {
          DataType: "String",
          StringValue: uuid,
        },
        eventType: {
          DataType: "String",
          StringValue: "ACTION_GENERIC",
        },
        iun: {
          DataType: "String",
          StringValue: action.iun,
        },
        publisher: {
          DataType: "String",
          StringValue: "deliveryPush",
        },
      },
      MessageBody: JSON.stringify(copiedAction),
    };
    return message;
}
  
module.exports = { writeMessagesToQueue };

