package com.berhan.elasticsearch.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import com.berhan.elasticsearch.dataloader.Tweet;
import com.berhan.elasticsearch.domain.TweetFilter;
import com.berhan.elasticsearch.repository.TweetRepository;

@Service
public class TweetServiceImpl implements TweetService {

	private TweetRepository tweetRepository;
	private ElasticsearchRestTemplate elasticSearchRestTemplate;

	public TweetServiceImpl(TweetRepository tweetRepo, ElasticsearchRestTemplate restTemplate) {
		this.tweetRepository = tweetRepo;
		this.elasticSearchRestTemplate = restTemplate;
	}

	@Override
	public List<Tweet> getTweets() {
		return tweetRepository.findAll();
	}

	@Override
	public Optional<Tweet> getTweetById(String id) {
		return tweetRepository.findById(id);
	}

	@Override
	public List<Tweet> filterTweets(TweetFilter filter) {
		Criteria criteria = new Criteria();
		if(filter.getTimeFrom() != null)
			criteria.and("createdAt")
				.greaterThanEqual(filter.getTimeFrom());
		if(filter.getTimeTo() !=null)
			criteria.and("createdAt").lessThanEqual(filter.getTimeTo());
		if (filter.getSelectedTags() != null) {
			criteria.and("hashtags").in(filter.getSelectedTags());
		}
		if (filter.getCenter() != null)
			criteria.and("geoLocation").within(filter.getCenter(), filter.getRadius());

		Query query = new CriteriaQuery(criteria);
		return elasticSearchRestTemplate.search(query, Tweet.class).get().map(SearchHit::getContent)
				.collect(Collectors.toList());
	}

	@Override
	public Optional<List<Tweet>> searchByText(String tweetTxt) {

		Criteria criteria = new Criteria("txt").matches(tweetTxt);

		Query query = new CriteriaQuery(criteria);

		return Optional.ofNullable(elasticSearchRestTemplate.search(query, Tweet.class).get().map(SearchHit::getContent)
				.collect(Collectors.toList()));

	}

}
