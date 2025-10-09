package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class InfraApp {
    public static void main(final String[] args) {
        App app = new App();

        String deployEnv = System.getenv("DEPLOY_ENV");
        String region = System.getenv("AWS_DEFAULT_REGION");


        if ("test".equalsIgnoreCase(deployEnv)) {
            new InfraStack(app, "dummy", StackProps.builder()
                    .env(Environment.builder()
                            .account("000000000000")
                            .region(region)
                            .build())
                    .build());
        }else{
            new InfraStack(app, "LambdaCDKStack", StackProps.builder().build());
        }

        app.synth();
    }
}

