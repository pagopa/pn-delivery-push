const axios = require("axios");

exports.createProgressResponseV25 = async (responseBody) => {
    console.debug("createProgressResponseV25")

    if (responseBody.newStatus === 'RETURNED_TO_SENDER') {
        const notification = await getNotificationFromDelivery(responseBody)
        const statusHistory = await getStatusHistory(notification)
        responseBody.newStatus = statusHistory.data.notificationStatusHistory[statusHistory.data.notificationStatusHistory.length-2].status
    }

    return responseBody;
}

async function getNotificationFromDelivery(responseBody) {
    const deliveryPrivateUrl = `${process.env.BASE_PATH}/delivery-private/notifications/${responseBody.iun}`;
    try {
      const notification = await axios.get(deliveryPrivateUrl, {
        headers: {}
      });
      return notification;
    
    } catch (e) {
      console.error("Call to delivery-private failed with status %s, iun : %s ", e.response.status, responseBody.iun);
      e.message = `Call to delivery-private failed with status ${e.response.status}, iun : ${responseBody.iun}`
      throw e;
    }
}

async function getStatusHistory(notification) {    
    const deliveryPushPrivateUrl = `${process.env.BASE_PATH}/delivery-push-private/${notification.data.iun}/history`;
    
    try {
      const statusHistory = await axios.get(deliveryPushPrivateUrl, {
      headers: {},
      params: {
        createdAt: notification.data.sentAt,
        numberOfRecipients: notification.data.recipients.length
        }
      });
      if(statusHistory.data.notificationStatusHistory.length === 0) {
        throw new Error(`Call to delivery-private failed, [iun : ${notification.data.iun}, createdAt : ${notification.data.sentAt}, numberOfRecipients : ${notification.data.recipients.length} ]`)
      }
      return statusHistory;
    
    } catch (e) {
      console.error("Call to delivery-private failed, [iun : %s, createdAt : %s, numberOfRecipients : %s ]", notification.data.iun, notification.data.sentAt, notification.data.recipients.length)
      throw e;
    }
}