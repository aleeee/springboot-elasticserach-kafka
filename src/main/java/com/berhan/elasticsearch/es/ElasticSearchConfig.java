package com.berhan.elasticsearch.es;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.expression.ParseException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.DateDeserializer;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class ElasticSearchConfig extends AbstractElasticsearchConfiguration{
	@Value("${elastic.url}")
	String elasticSearchUrl;
	@Value("${elastic.port}")
	int elasticSearchPort;
	 private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	@Bean
	@Override
	public RestHighLevelClient elasticsearchClient() {
		RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(new HttpHost(elasticSearchUrl, elasticSearchPort, "http")));
		
		return client;
	}
	@Bean
	@Primary
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm a z");
		mapper.setDateFormat(df);		
		
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		DateSerializer dateSerializer = new DateSerializer();
		dateSerializer.withFormat(true, df);
		DateDeserializer dateDeserializer = new DateDeserializer();
		dateSerializer.withFormat(true, df);
		
		JavaTimeModule module = new JavaTimeModule();
		module.addSerializer(Date.class,dateSerializer);
		module.addDeserializer(Date.class,dateDeserializer);
		mapper.registerModule(module);
		return mapper;

	}

	   


	    @Override
	    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
	        return new ElasticsearchCustomConversions(Arrays.asList(DateToStringConverter.INSTANCE, StringToDateConverter.INSTANCE));
	    }

	    @WritingConverter
	    enum DateToStringConverter implements Converter<Date, String> {
	        INSTANCE;
	        @Override
	        public String convert(Date date) {
	            return formatter.format(date);
	        }
	    }

	    @ReadingConverter
	    enum StringToDateConverter implements Converter<String, Date> {
	        INSTANCE;
	        @Override
	        public Date convert(String s) {
	            try {
	                return formatter.parse(s);
	            } catch (ParseException | java.text.ParseException e) {
	                return null;
	            }
	        }
	    }

		

}
