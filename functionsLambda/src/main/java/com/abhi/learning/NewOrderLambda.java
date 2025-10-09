package com.abhi.learning;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;


/**
 * Hello world!
 */
public class NewOrderLambda implements RequestHandler<SQSEvent, String> {

    private static final String SNS_TOPIC_ARN = System.getenv("SNS_TOPIC_ARN");

    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {

        try (SnsClient snsClient = SnsClient.builder()
                .region(Region.AP_SOUTH_1) // Change region if needed
                .build()) {

            String message = sqsEvent.getRecords().get(0).getBody();
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
