package com.berhan.elasticsearch.dataloader;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

@Component
public class TweetCollector  {
	private static final Logger logger = LoggerFactory.getLogger(TweetCollector.class);
	@Value("${twitter.api.key}")
	String apiKey;
	@Value("${twitter.api.secret}")
	String apiSecret;
	@Value("${twitter.auth.token}")
	String authToken;
	@Value("${twitter.access.token}")
	String accessToken;
	@Value("${twitter.access.secret}")
	String accessTokenSecret;
	@Value("${tweets.filter.keys}")
	String[] keyWords;
	@Value("${elastic.index.name}")
	String tweetsIndexName;
	@Value("${bootstrap.servers}")
	String brokerList;
	@Value("${destination.topics}")
	String destinationTopics;
	@Value("${stream.duration}")
	int streamDuration;

	@Autowired
	RestHighLevelClient esClient;
	TwitterStream twitterStream =null;
	@PostConstruct
	public void initStream() {
		 twitterStream = initTwitterStream();
	}
	public void stream(String destination) throws Exception {

		// create tweet queue
		final LinkedBlockingQueue<Status> queue = new LinkedBlockingQueue<>(10);
		// prepare the stream
		
		streamTweets(queue,twitterStream);
		ObjectMapper mapper = new ObjectMapper();

		if(destination.equalsIgnoreCase("elasticsearch")) {
			streamToEs(queue, mapper);
		}else {
			streamToKafka(queue, mapper);
		}
	}
	
	public void shutdown() {
		twitterStream.shutdown();
	}

	private void streamToEs(final LinkedBlockingQueue<Status> queue, ObjectMapper mapper)
			throws InterruptedException, IOException {
		if (!indexExists(tweetsIndexName))
			createIndex(tweetsIndexName);
		BulkProcessor bulkProcessor = bulk();
		
		while (!Thread.currentThread().isInterrupted()) {

			Status status = queue.poll();

			if (status == null) {
				System.out.println("waiting...");
				Thread.sleep(1000);
			} else {
				Tweet tweet = new Tweet();
				if (status.getHashtagEntities() != null) {
					tweet.setHashtags(Arrays.asList(status.getHashtagEntities()).stream().map(tag -> tag.getText())
							.collect(Collectors.toList()));
				}

				tweet.setCreatedAt(status.getCreatedAt());
				tweet.setGeoLocation(status.getGeoLocation());
				tweet.setTxt(status.getText());
				tweet.setFavorites(status.getFavoriteCount());
				tweet.setRetweets(status.getRetweetCount());
				tweet.setLanguage(status.getLang());
				tweet.setSource(status.getSource());
				logger.info("tweet...{}", tweet);
				IndexRequest req = new IndexRequest(tweetsIndexName).source(mapper.writeValueAsString(tweet),
						XContentType.JSON);
				bulkProcessor.add(req);

			}
		}
	}

	private TwitterStream initTwitterStream() {
		// config
		ConfigurationBuilder confBuilder = new ConfigurationBuilder();

		Configuration conf = confBuilder.setDebugEnabled(true).setOAuthAccessToken(accessToken)
				.setOAuthAccessTokenSecret(accessTokenSecret).setOAuthConsumerKey(apiKey)
				.setOAuthConsumerSecret(apiSecret).build();

		// make twitter stream
		return new TwitterStreamFactory(conf).getInstance();

	}

	private void streamTweets(BlockingQueue<Status> queue, TwitterStream twitterStream) {
		

		// create the query filter

		FilterQuery query = new FilterQuery().track(keyWords);

		twitterStream.addListener(new StatusListener() {

			@Override
			public void onException(Exception ex) {
				ex.printStackTrace();

			}

			@Override
			public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStatus(Status status) {
				queue.offer(status);

			}

			@Override
			public void onStallWarning(StallWarning warning) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onScrubGeo(long userId, long upToStatusId) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
				// TODO Auto-generated method stub

			}
		});

		twitterStream.filter(query);

	}

	private boolean indexExists(String indexName) throws IOException {
		GetIndexRequest request = new GetIndexRequest(indexName);

		return esClient.indices().exists(request, RequestOptions.DEFAULT);
	}

	private void createIndex(String indexName) throws IOException {
		CreateIndexRequest request = new CreateIndexRequest(indexName);
		esClient.indices().create(request, RequestOptions.DEFAULT);
	}

	private BulkProcessor bulk() {
		try {

			BulkProcessor.Listener listener = new BulkProcessor.Listener() {
				@Override
				public void beforeBulk(long executionId, BulkRequest request) {
					int numberOfActions = request.numberOfActions();
					logger.info("Executing bulk [{}] with {} requests", executionId, numberOfActions);
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
					if (response.hasFailures()) {
						logger.warn("Bulk [{}] executed with failures", executionId);
						logger.warn("Bulk [{}] execution result", response);
					} else {
						logger.info("Bulk [{}] completed in {} milliseconds", executionId,
								response.getTook().getMillis());
					}
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
					logger.error("Failed to execute bulk", failure);
				}
			};

			BiConsumer<BulkRequest, ActionListener<BulkResponse>> bulkConsumer = (request, bulkListener) -> esClient
					.bulkAsync(request, RequestOptions.DEFAULT, bulkListener);

			BulkProcessor.Builder builder = BulkProcessor.builder(bulkConsumer, listener);

			builder.setBulkActions(20);
			builder.setConcurrentRequests(0);
			builder.setFlushInterval(TimeValue.timeValueSeconds(5));
			return builder.build();
		} catch (Exception e) {
			logger.error("Error bulk index {}", e);
			return null;
		}

	}

	private void streamToKafka( LinkedBlockingQueue<Status> queue, ObjectMapper mapper)
			throws InterruptedException, JsonProcessingException {
		Producer<String, String> producer = new KafkaProducer<>(getProperties());
		
		while (!Thread.currentThread().isInterrupted()) {
			Status status = queue.poll();
			if (status == null) {
				System.out.println("waiting...");
				Thread.sleep(1000);
			} else {
				logger.info("trend...{}", status);

				Tweet tweet = new Tweet();
				if (status.getHashtagEntities() != null) {
					tweet.setHashtags(Arrays.asList(status.getHashtagEntities()).stream().map(tag -> tag.getText())
							.collect(Collectors.toList()));
				}

				tweet.setCreatedAt(status.getCreatedAt());
				tweet.setGeoLocation(status.getGeoLocation());
				tweet.setTxt(status.getText());
				tweet.setFavorites(status.getFavoriteCount());
				tweet.setRetweets(status.getRetweetCount());
				tweet.setLanguage(status.getLang());
				tweet.setSource(status.getSource());
				logger.info("tweet...{}", tweet);
				producer.send(new ProducerRecord<String, String>(destinationTopics, mapper.writeValueAsString(tweet)));

			}

		}
	}

	private Properties getProperties() {
		Properties props = new Properties();
		props.put("bootstrap.servers", brokerList);
		props.put("acks", "all");
		props.put("retries", "0");
		props.put("batch.size", 16384);
		props.put("linger.ms", 1);
		props.put("group.id", "twitter-group1");
		props.put("buffer.memory", 33554422);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		return props;
	}
}
