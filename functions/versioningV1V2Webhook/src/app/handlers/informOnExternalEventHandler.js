const EventHandler  = require('./baseHandler.js');
class InformOnExternalEventHandler extends EventHandler {
    constructor() {
        super();
    }

    checkOwnership(event, context){
        const {path, httpMethod} = event;
            return path === '/delivery-progresses/events' && httpMethod.toUpperCase() === 'POST';
    }

    async handlerEvent(event, context) {
        throw new Error("NOT YET IMPLEMENTED")
    }
}

module.exports = InformOnExternalEventHandler;