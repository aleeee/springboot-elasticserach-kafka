package com.berhan.elasticsearch.service;

import java.util.List;
import java.util.Optional;

import com.berhan.elasticsearch.dataloader.Tweet;
import com.berhan.elasticsearch.domain.TweetFilter;

public interface TweetService {
	List<Tweet> getTweets();
	Optional<Tweet> getTweetById(String id);
	
	Optional<List<Tweet>> searchByText(String tweetTxt);

	List<Tweet> filterTweets(TweetFilter filter);

}
