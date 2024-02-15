exports.generateProblem = function(status, message){
    return {
        status: status,
        errors: [
            {
                code: message
            }
        ]
    }
}