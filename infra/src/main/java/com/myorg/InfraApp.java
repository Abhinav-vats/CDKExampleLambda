package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;
import java.util.Arrays;

public class InfraApp {
    public static void main(final String[] args) {
        App app = new App();

        String deployEnv = System.getenv("DEPLOY_ENV");
        String region = System.getenv("AWS_DEFAULT_REGION");

        System.out.println(deployEnv);


        if ("test".equalsIgnoreCase(deployEnv)) {
            new InfraStack(app, "dummy", StackProps.builder()
                    .env(Environment.builder()
                            .account("000000000000")
                            .region(region)
                            .build())
                    .build());

            SnsClient snsClient = SnsClient.builder()
                    .endpointOverride(URI.create(System.getenv("LOCALSTACK_ENDPOINT")))
                    .region(Region.of(System.getenv("CDK_DEPLOY_REGION")))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test","test")
                    ))
                    .build();

        }else{
            new InfraStack(app, "LambdaCDKStack", StackProps.builder().build());
        }

        app.synth();
    }
}

