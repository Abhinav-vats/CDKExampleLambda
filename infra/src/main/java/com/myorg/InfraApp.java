package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;

public class InfraApp {
    public static void main(final String[] args) {
        App app = new App();


        new InfraStack(app, "LambdaCDKStack", StackProps.builder().build());


        app.synth();
    }
}

