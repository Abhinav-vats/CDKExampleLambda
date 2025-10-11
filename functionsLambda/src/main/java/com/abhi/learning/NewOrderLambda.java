package com.abhi.learning;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;


/**
 * Hello world!
 */
@SuppressWarnings("unchecked")
public class NewOrderLambda implements RequestHandler<SQSEvent, String> {

    private static final String SNS_TOPIC_ARN = System.getenv("SNS_TOPIC_ARN");

    private static final Logger logger = LoggerFactory.getLogger(NewOrderLambda.class);


    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {

        final ObjectMapper mapper= new ObjectMapper();

        try (SnsClient snsClient = SnsClient.builder()
                .region(Region.of(sqsEvent.getRecords().get(0).getAwsRegion())) // Change region if needed
                .build()) {

            String bodyStr = sqsEvent.getRecords().get(0).getBody();


            Map<String, Object> body = mapper
                    .readValue(bodyStr, new TypeReference<HashMap<String, Object>>() {});

            if(body.get("detail") instanceof HashMap<?,?>) {

                Map<String, Object> msg = mapper.convertValue( body.get("detail"), HashMap.class);

                String isOrNot = Boolean.parseBoolean(String.valueOf(msg.get("isStudent"))) ? "" : " not ";

                if(msg.containsKey("name") && msg.containsKey("age") && msg.containsKey("message")) {
                    String message = String
                            .format("Hi %s, whose age is %d, and is%s student, is sending you a message, %s", msg.get("name"),
                                    Integer.parseInt(String.valueOf(msg.get("age"))), isOrNot, msg.get("message"));


                    PublishRequest publishRequest = PublishRequest.builder()
                            .topicArn(SNS_TOPIC_ARN)
                            .message(message)
                            .build();

                    PublishResponse response = snsClient.publish(publishRequest);
                    context.getLogger().log("Message published successfully. MessageId: " + response.messageId());
                }else{
                    throw new Exception("Expected Keys: ['name', 'age', 'message']; Actual Keys: "+msg.keySet());
                }
            }
        } catch (Exception e) {
            logger.error("Error publishing to SNS: " + e.getMessage());
            return "SNS Publish Failed: " + e.getMessage();
        }

            context.getLogger().log(sqsEvent.getRecords().toString());
        return sqsEvent.getRecords().get(0).getBody();
    }



}
