package com.microservices.demo.twitter.to.kafka.service.runner.impl;

import com.microservices.demo.twitter.to.kafka.service.config.TwitterToKafkaServiceConfigData;
import com.microservices.demo.twitter.to.kafka.service.listener.TwitterKafkaStatusListener;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import twitter4j.JSONException;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "twitter-to-kafka-service.enable-v2-tweets", havingValue = "true", matchIfMissing = true)
public class TwitterV2StreamHelper {

    private static final Logger LOG = LoggerFactory.getLogger(TwitterV2StreamHelper.class);

    private final TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;

    private final TwitterKafkaStatusListener twitterKafkaStatusListener;

    private static final String tweetAsRawJson = "{\"created_at\":\"{0}\",\"id\":\"{1}\",\"text\":\"{2}\",\"user\":{\"id\":\"{3}\"}}";

    private static final String TWITTER_STATUS_DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";

    public TwitterV2StreamHelper(TwitterToKafkaServiceConfigData configData, TwitterKafkaStatusListener statusListener) {
        this.twitterToKafkaServiceConfigData = configData;
        this.twitterKafkaStatusListener = statusListener;
    }

    public void connectStream(String bearerToken) throws IOException, URISyntaxException, TwitterException, JSONException {
        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        URIBuilder uriBuilder = new URIBuilder(twitterToKafkaServiceConfigData.getTwitterV2BaseUrl());

        HttpGet httpGet = new HttpGet(uriBuilder.build());

        httpGet.setHeader("Authorization", String.format("Bearer %s", bearerToken));

        HttpResponse response = httpClient.execute(httpGet);

        HttpEntity entity = response.getEntity();

        if (null != entity) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                if (!line.isEmpty()) {
                    String tweet = getFormattedTweet(line);
                    Status status = null;
                    try{
                        status = TwitterObjectFactory.createStatus(tweet);
                    }catch (TwitterException e){
                        LOG.error("Could not create status for text: {}", tweet, e);
                    }
                    if(status!=null){
                        twitterKafkaStatusListener.onStatus(status);
                    }
                }
            }
        }
    }

    void setupRules(String bearerToken, Map<String, String> rules) throws IOException, URISyntaxException{
        List<String> existingRules = getRules(bearerToken);
        if(existingRules.size()>0){
            deleteRules(bearerToken, existingRules);
        }
        createRules(bearerToken, rules);
        LOG.info("Created rules for twitter stream {}", rules.keySet().toArray());
    }

    private void createRules(String bearerToken, Map<String, String> rules) throws URISyntaxException, IOException{

    }

    private String getFormattedTweet(String data) {
        JSONObject jsonData = (JSONObject) new JSONObject(data).get("data");
        String[] params = new String[]{
                ZonedDateTime.parse(jsonData.get("created_at").toString()).withZoneSameInstant(ZoneId.of("UTC"))
                        .format(DateTimeFormatter.ofPattern(TWITTER_STATUS_DATE_FORMAT, Locale.ENGLISH)),
                jsonData.get("id").toString(),
                jsonData.get("text").toString().replaceAll("\"", "\\\\\""),
                jsonData.get("author_id").toString(),
        };
        return formatTweetAsJsonWithParams(params);
    }

    private String formatTweetAsJsonWithParams(String[] params) {
        String tweet = tweetAsRawJson;

        for (int i = 0; i < params.length; i++) {
            tweet = tweet.replace("{" + i + "}", params[i]);
        }
        return tweet;
    }
}
