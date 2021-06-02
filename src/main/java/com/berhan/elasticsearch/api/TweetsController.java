package com.berhan.elasticsearch.api;

import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import com.berhan.elasticsearch.dataloader.Tweet;
import com.berhan.elasticsearch.dataloader.TweetCollector;
import com.berhan.elasticsearch.domain.TweetFilter;
import com.berhan.elasticsearch.service.TweetService;

@RestController
@RequestMapping("api/v1/tweets")
public class TweetsController {
	
	Logger log = LoggerFactory.getLogger(TweetsController.class);
	
	@Autowired
	TweetService tweetService;
	@Autowired
	TweetCollector tweetCollector;
	
	@Value("${stream.duration}")
	int streamDuration;
	
	final Thread[] asyncTaskThread = new Thread[1];
	String dest;
	@GetMapping
	public List<Tweet> getTweets(){
		return tweetService.getTweets();
	}
	
	@GetMapping("/{id}")
	public Tweet getTweet(@PathVariable("id") String id){
		return tweetService.getTweetById(id).orElse(null);
	}
	@GetMapping("/tweet/{txt}")
	public List<Tweet> searchTweetByTxt(@PathVariable("txt") String txt){
		return tweetService.searchByText(txt).orElse(null);
	}
	
	@PostMapping("/filter")
    public List<Tweet> filterTweets(@RequestBody  TweetFilter tweetFilter) {
        return this.tweetService.filterTweets(tweetFilter);
    }
	
	@GetMapping("/stream/{destination}")
    public WebAsyncTask<Void>stream(@PathVariable("destination") String dest){
		this.dest=dest;
        log.info("streaming to {}", dest);
        WebAsyncTask<Void> webAsyncTask = new WebAsyncTask<Void>(streamDuration * 1000, callableTask);
        webAsyncTask.onTimeout(onTimeoutcallableTask);
        webAsyncTask.onCompletion(async);
        return webAsyncTask;
    }
	
	Callable<Void> callableTask = () -> {
		asyncTaskThread[0] = Thread.currentThread();
		log.info("starting...");
		tweetCollector.stream(dest);
		return null;
		};
	Callable<Void> onTimeoutcallableTask = () -> {
		log.info("timeout");
		asyncTaskThread[0].interrupt();
		tweetCollector.shutdown();
		return null;};
	Runnable async = () ->{
		log.info("completed");
		tweetCollector.shutdown();
	};
}
