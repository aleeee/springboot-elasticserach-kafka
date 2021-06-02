package com.berhan.elasticsearch;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.Date;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import com.berhan.elasticsearch.dataloader.Tweet;
import com.berhan.elasticsearch.domain.TweetFilter;
import com.berhan.elasticsearch.service.TweetService;

import twitter4j.GeoLocation;
@ExtendWith(SpringExtension.class)
@SpringBootTest
class ElasticsearchApplicationTests {
	  private static final String url = "docker.elastic.co/elasticsearch/elasticsearch:7.2.0";
	@ClassRule
	public static ElasticsearchContainer container = new ElasticsearchContainer(url);
	

	@BeforeAll
	public static void before() {
		container.start();
		System.setProperty("elastic.url",
				container.getContainerIpAddress());
		System.setProperty("elastic.port",String.valueOf(container.getMappedPort(9200)));
	}

	@Autowired
	TweetService tweetService;
	@Autowired
	ElasticsearchRestTemplate elasticSearchRestTemplate;

	@BeforeEach
	public void init() {

		Tweet tweet = new Tweet();
		tweet.setId("1");
		tweet.setHashtags(Collections.singletonList("Elastic"));
		tweet.setCreatedAt(new Date());
		tweet.setTxt("spring boot with elasticsearch ");
		tweet.setFavorites(5);
		tweet.setRetweets(2);
		tweet.setLanguage("en");
		tweet.setSource("<a href=\\\"https://mobile.twitter.com\\\" rel=\\\"nofollow\\\">Twitter Web App</a>");

		elasticSearchRestTemplate.save(tweet);

	}

	@Test
	public void findByIdTest() {

		assertNotNull(tweetService.getTweetById("1"));
	}
	
	@Test
	public void searchByTxtTest() {
		
		assertNotNull(tweetService.searchByText("spring"));
	}
	@Test
	public void findAllTest() {
		
		assertNotNull(tweetService.getTweets());
	}
	@Test
	public void filterTweetsTest() {
		TweetFilter filter = new TweetFilter();
		filter.setSelectedTags(Collections.singletonList("Elastic"));
		assertNotNull(tweetService.filterTweets(filter));
	}

}
