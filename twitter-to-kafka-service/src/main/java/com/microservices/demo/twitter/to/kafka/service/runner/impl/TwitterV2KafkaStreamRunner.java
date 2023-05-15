package com.microservices.demo.twitter.to.kafka.service.runner.impl;

import com.microservices.demo.twitter.to.kafka.service.config.TwitterToKafkaServiceConfigData;
import com.microservices.demo.twitter.to.kafka.service.runner.StreamRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import twitter4j.TwitterException;

@Component
@ConditionalOnProperty(name = "twitter-to-kafka-service.enable-v2-tweets", havingValue = "true", matchIfMissing = true)
public class TwitterV2KafkaStreamRunner implements StreamRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterV2KafkaStreamRunner.class);

    private final TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;

    private final TwitterV2StreamHelper twitterV2StreamHelper;

    public TwitterV2KafkaStreamRunner(TwitterToKafkaServiceConfigData configData, TwitterV2StreamHelper streamHelper) {
        this.twitterToKafkaServiceConfigData = configData;
        this.twitterV2StreamHelper = streamHelper;
    }

    @Override
    public void start() throws TwitterException {
        String bearerToken = twitterToKafkaServiceConfigData.getTwitterV2BearerToken();
        if (bearerToken != null) {
            twitterV2StreamHelper.se
        } else {
            LOGGER.error("There was a problem getting your bearer token. " +
                    "Please make sure you set the TWITTER_BEARER_TOKEN environment variable");
            throw new RuntimeException("There was a problem getting your bearer token. " +
                    "Please make sure you set the TWITTER_BEARER_TOKEN environment variable");
        }
    }
}
