package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.EventBus;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.targets.SqsQueue;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.logs.*;
import software.amazon.awscdk.services.logs.destinations.LambdaDestination;
import software.amazon.awscdk.services.sns.Subscription;
import software.amazon.awscdk.services.sns.SubscriptionProtocol;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

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
        Topic snsTopic = Topic.Builder.create(this, "MySnsTopic")
                .topicName("new-order")
                .displayName("My SNS Topic")
                .build();



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


        Topic errorTopic = Topic.Builder.create(this, "MyErrorTopic")
                .topicName("errorLog")
                .displayName("My Error Topic")
                .build();



        ILogGroup errorLogGroup = LogGroup.Builder.create(this, "error-log-group")
                .retention(RetentionDays.FIVE_DAYS)
                .logGroupClass(LogGroupClass.STANDARD)
                .logGroupName("errorLogGroup")
                .build();

        Function errorForwarderLambda = Function.Builder.create(this, "error-log-forwarder")
                .runtime(Runtime.JAVA_17)
                .handler("com.abhi.learning.ErrorLogForwarder")
                .memorySize(512)
                .environment(Map.of(
                        "SNS_TOPIC_ARN", errorTopic.getTopicArn(),
                        "AWS_REGION_FN", this.getRegion()
                ))
                .timeout(Duration.seconds(50))
                .logGroup(errorLogGroup)
                .functionName("ErrorLogForwarder")
                .code(Code.fromAsset("../assets/ErrorForwarder.jar")).build();

        errorTopic.grantPublish(errorForwarderLambda);

        ILogGroup logGroup = LogGroup.fromLogGroupName(this, "AppLogGroup", lambdaFunction.getLogGroup().getLogGroupName());

        SubscriptionFilter.Builder.create(this, "ErrorLogsFilter")
                .logGroup(logGroup)
                .destination(new LambdaDestination(errorForwarderLambda))
                .filterPattern(software.amazon.awscdk.services.logs.FilterPattern.literal("{ $.level = \"ERROR\" }"))
                .build();



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
