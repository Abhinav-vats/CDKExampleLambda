package com.abhi.learning;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class NewOrderLambda implements RequestHandler<SQSEvent, String> {

    private static final String SNS_TOPIC_ARN = System.getenv("SNS_TOPIC_ARN");

    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {

        final ObjectMapper mapper= new ObjectMapper();

        try (SnsClient snsClient = SnsClient.builder()
                .region(Region.of(System.getenv("AWS_REGION"))) // Change region if needed
                .build()) {

            String bodyStr = sqsEvent.getRecords().get(0).getBody();

            context.getLogger().log(bodyStr, LogLevel.INFO);

            Map<String, Object> body = mapper
                    .convertValue(bodyStr, new TypeReference<HashMap<String, Object>>() {});

            Map<String, Object> msg = mapper
                    .convertValue(body.get("detail"),new TypeReference<HashMap<String, Object>>(){});

            String isOrNot = Boolean.parseBoolean(String.valueOf(msg.get("isStudent")))?"":" not ";

            String message = String
                    .format("Hi %s, whose age is %d, and is%s student, is sending you a message, %s", msg.get("name"),
                            Integer.parseInt(String.valueOf(msg.get("age"))), isOrNot, msg.get("message"));

            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(SNS_TOPIC_ARN)
                    .message(message)
                    .build();

            PublishResponse response = snsClient.publish(publishRequest);
            context.getLogger().log("Message published successfully. MessageId: " + response.messageId());
        } catch (Exception e) {
            context.getLogger().log("Error publishing to SNS: " + e.getMessage());
            return "SNS Publish Failed: " + e.getMessage();
        }

            context.getLogger().log(sqsEvent.getRecords().toString());
        return sqsEvent.getRecords().get(0).getBody();
    }
}
