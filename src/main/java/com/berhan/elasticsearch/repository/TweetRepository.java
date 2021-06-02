package com.berhan.elasticsearch.repository;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.berhan.elasticsearch.dataloader.Tweet;

@Repository
public interface TweetRepository extends ElasticsearchRepository<Tweet, String> {
	
	List<Tweet> findAll();
}
