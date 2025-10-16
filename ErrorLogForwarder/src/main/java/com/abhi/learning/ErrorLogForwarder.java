package com.abhi.learning;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Hello world!
 */
@SuppressWarnings("unchecked")
public class ErrorLogForwarder implements RequestHandler<CloudWatchLogsEvent, Void> {
    private static final String SNS_TOPIC_ARN = System.getenv("SNS_TOPIC_ARN");
    private static final Logger logger = LoggerFactory.getLogger(ErrorLogForwarder.class);
    @Override
    public Void handleRequest(CloudWatchLogsEvent event, Context context) {
        try (SnsClient snsClient = SnsClient.builder()
                .region(Region.of(System.getenv("AWS_REGION_FN"))) // Change region if needed
                .build()) {

            context.getLogger().log(event.toString());
            String bodyStr =  event.getAwsLogs().getData();

            byte[] decodedBytes = Base64.getDecoder().decode(bodyStr);
            String decompressedJson = decompressGzip(decodedBytes);
            context.getLogger().log(decompressedJson, LogLevel.INFO);


            final ObjectMapper mapper= new ObjectMapper();
            Map<String, Object> messageMap = mapper.readValue(decompressedJson, new TypeReference<HashMap<String, Object>>() {});

            List<Map<String, Object>> logEvents = (List<Map<String, Object>>) messageMap.get("logEvents");

            StringBuilder messageBuilder = new StringBuilder();

            for (Map<String, Object> logEvent : logEvents) {

                String messageLog = (String) logEvent.get("message");
                Map<String, Object> internalMessageMap = mapper.readValue(messageLog, new TypeReference<HashMap<String, Object>>() {});

                messageBuilder.append(internalMessageMap.get("message"));
                messageBuilder.append(" Generated from : ");
                messageBuilder.append(internalMessageMap.get("logger_name"));
                messageBuilder.append("\n");


            }

            String message = messageBuilder.toString();
            context.getLogger().log(message, LogLevel.INFO);
//
//            Map<String, Object> internalMessageMap = mapper.readValue(message, new TypeReference<HashMap<String, Object>>() {});
//
//            String messageError = (String) internalMessageMap.get("message");

            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(SNS_TOPIC_ARN)
                    .message(message)
                    .build();

            PublishResponse response = snsClient.publish(publishRequest);
            logger.info("Message published successfully. MessageId: " + response.messageId());
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
        return null;
    }

    private static String decompressGzip(byte[] compressed) throws IOException {
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
             InputStreamReader reader = new InputStreamReader(gis, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(reader)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        String data  = "H4sIAAAAAAAA/1WST0/jMBDFv0o0WqmXtB3HcRx7L1SQ5QALUtMTpKrcxKQW+SfbLaCq3x01hUV7svz8mzdvRj5Cq51TtV59DBok3CxWi83fLM8XtxmE0L912oKEJE04i0mKhAoIoenrW9vvB5AwV29u3qh2W6n5g357tJW29+P1wuXeatWChAgjNic4J2T+/Ot+scry1VrTkpIXQUuOaVxuaaqT6kWVtBQlRc4QQnD7rSutGbzpuz+m8do6kM9waXF9c5d7Vb5OM2t7e9/X7oJQseDZNYunH+Xu/VWz+okPBtZjoOygO3/2OIKpQAIVEaOC8yRORJRGMU8RU0HSOKFRlFKWcMbiKEEUIsUEmUCCMeMcQvCm1c6rdgBJeIIRiogyjEX4vVSQcCzg6h9XgCzGRUwJTglZESERJWUzjEXEYk7SpwLCAq4O2jrTdyNPRunLcVTGaYNhv22M25muDnwf5A+5DLL3QZdeV8Gd/nAyeJ50qtWTMJioejy+TCbr38Gi9HvVfINnzoXB17sLA1XrMDAu9/tKd349Rmj6utZ2c2bHGGXfztR2Z2aNVrYzXT37/wOMRX5ntap+ilpluoubPujmMs5y+bj80TYH1ezPcIyIeCo6OK1Pn8pJB+GoAgAA";
        byte[] decodedBytes = Base64.getDecoder().decode(data);
        try {
            String decompressedJson = decompressGzip(decodedBytes);

            System.out.println(decompressedJson);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
