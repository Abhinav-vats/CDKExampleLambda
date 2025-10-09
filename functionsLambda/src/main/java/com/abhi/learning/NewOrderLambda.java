package com.abhi.learning;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;


/**
 * Hello world!
 */
public class NewOrderLambda implements RequestHandler<SQSEvent, String> {


    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {

        context.getLogger().log(sqsEvent.getRecords().toString());
        return sqsEvent.getRecords().get(0).getBody();
    }
}
