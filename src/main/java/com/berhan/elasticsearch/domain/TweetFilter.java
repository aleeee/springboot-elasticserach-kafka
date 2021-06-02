package com.berhan.elasticsearch.domain;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.elasticsearch.core.geo.GeoPoint;

public class TweetFilter {
	private LocalDateTime timeFrom;
	private LocalDateTime timeTo;
	private List<String> selectedTags;
	private GeoPoint center;
	private String radius;
	public LocalDateTime getTimeFrom() {
		return timeFrom;
	}
	public void setTimeFrom(LocalDateTime timeFrom) {
		this.timeFrom = timeFrom;
	}
	public LocalDateTime getTimeTo() {
		return timeTo;
	}
	public void setTimeTo(LocalDateTime timeTo) {
		this.timeTo = timeTo;
	}
	public List<String> getSelectedTags() {
		return selectedTags;
	}
	public void setSelectedTags(List<String> selectedTags) {
		this.selectedTags = selectedTags;
	}
	public GeoPoint getCenter() {
		return center;
	}
	public void setCenter(GeoPoint center) {
		this.center = center;
	}
	public String getRadius() {
		return radius;
	}
	public void setRadius(String radius) {
		this.radius = radius;
	}
	
	

}
