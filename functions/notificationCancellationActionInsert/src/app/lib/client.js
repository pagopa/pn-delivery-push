const axios = require('axios');

const insertAction = async (events) => {
    const summary = {
        insertions: 0,
        errors: [],
    };

    const BASE_URL = process.env.ACTION_MANAGER_BASE_URL;
    if (!BASE_URL) {
        throw new Error("ACTION_MANAGER_BASE_URL env is not set");   
    }

    const url = `${BASE_URL}/action-manager-private/action`;

    for(const event of events) {
        let action = buildAction(event);
        try {
            const response = await axios.post(url, action, {
                headers: {
                    'Content-Type': 'application/json',
                },
            });

            summary.insertions++;
            console.log(`Action inserted successfully: actionId=${event.actionId}`);
        } catch (error) {
            console.error("Error putting action:", error);
            if( error.response.status === 409) {
                console.warn(`Action already exists: actionId=${event.actionId}`);
                continue; // skip this action, it already exists
            }
            summary.errors.push(event);
        }
    }

    return summary;
}

const buildAction = (event) => {
    // won't set ttl, because it's handled by action-manager
    return {
        // key
        actionId: event.actionId,
        // other attributes
        iun: event.iun, // GSI
        type: event.type,
        notBefore: event.notBefore,
        timelineId: event.timelineId
    };
}

exports.insertAction = insertAction;