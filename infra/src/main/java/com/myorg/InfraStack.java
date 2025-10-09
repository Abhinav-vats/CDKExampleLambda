package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.events.EventBus;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.targets.SqsQueue;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.TopicProps;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.List;
import java.util.Map;

public class InfraStack extends Stack {

    public InfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);



        Queue queue = Queue.Builder.create(this, "workflow-queue")
                .queueName("NewOrderQueue")
                .visibilityTimeout(Duration.seconds(60))
                .build();
        Topic snsTopic = new Topic(this, "MySnsTopic",
                TopicProps.builder()
                        .topicName("my-topic")
                        .displayName("My SNS Topic")
                        .build());

        // CREATE LAMBDA FUNCTION
        Function lambdaFunction = Function.Builder.create(this, "new-order-lambda")
                .runtime(Runtime.JAVA_17)
                .handler("com.abhi.learning.NewOrderLambda")
                .memorySize(512)
                .timeout(Duration.seconds(50))
                .functionName("NewOrderLambda")
                .environment(Map.of(
                        "SNS_TOPIC_ARN", snsTopic.getTopicArn()
                ))
                .code(Code.fromAsset("../assets/function.jar")).build();

        queue.grantConsumeMessages(lambdaFunction);
        lambdaFunction.addEventSource(new SqsEventSource(queue));
        snsTopic.grantPublish(lambdaFunction);

        EventBus eventBus = EventBus.Builder.create(this, "NewOrderEventBus")
                .eventBusName("NewOrderAppBus")
                .build();

        Rule rule = Rule.Builder.create(this, "JsonUploadRule")
                .ruleName("NewOrderRule")
                .eventBus(eventBus)
                .eventPattern(EventPattern.builder()
                        .source(java.util.List.of("new.order"))
                        .detailType(java.util.List.of("NewOrderEvent"))
                        .build())

                .targets(List.of(new SqsQueue(queue)))
                .build();
    }
}
