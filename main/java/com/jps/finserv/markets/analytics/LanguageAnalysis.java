package com.jps.finserv.markets.analytics;

import com.ibm.watson.developer_cloud.discovery.v1.model.environment.GetEnvironmentRequest;
import com.ibm.watson.developer_cloud.discovery.v1.model.environment.GetEnvironmentResponse;
import com.ibm.watson.developer_cloud.discovery.v1.model.environment.GetEnvironmentsRequest;
import com.ibm.watson.developer_cloud.discovery.v1.model.environment.GetEnvironmentsResponse;
import com.ibm.watson.developer_cloud.discovery.v1.model.query.QueryRequest;
import com.ibm.watson.developer_cloud.discovery.v1.model.query.QueryResponse;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.ConceptsOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EntitiesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.Features;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.KeywordsOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.SentimentOptions;
import com.jps.finserv.markets.equities.Config;
import com.jps.finserv.markets.util.modReadFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.ibm.watson.developer_cloud.discovery.v1.Discovery;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LanguageAnalysis {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(LanguageAnalysis.class);

	public String analyzeLanguage(String url) {
		NaturalLanguageUnderstanding service = new NaturalLanguageUnderstanding(
				NaturalLanguageUnderstanding.VERSION_DATE_2017_02_27,	 "9b967bcc-3074-4ba2-ae02-5e197ae977f2",	"XMElVdlKc4nh");

		EntitiesOptions entitiesOptions = new EntitiesOptions.Builder().emotion(true)
				.sentiment(true)
				.limit(2)
				.build();

		KeywordsOptions keywordsOptions = new KeywordsOptions.Builder()
				.emotion(true)
				.sentiment(true)
				.limit(5)
				.build();

		//CategoriesOptions categoriesOptions = new CategoriesOptions();
		
		//ConceptsOptions conceptsOptions = new ConceptsOptions.Builder().limit(2).build();

		Features features = new Features.Builder()
				.entities(entitiesOptions)
				.keywords(keywordsOptions)
			//	.categories(categoriesOptions)
			//	.concepts(conceptsOptions)
				.build();

		AnalyzeOptions parameters = new AnalyzeOptions.Builder().url(url)
				.features(features)
				.build();

		AnalysisResults response = service
				.analyze(parameters)
				.execute();

		System.out.println(response);
		
		return response.toString();
	}
	
	public String analyzeLanguageSentiment(String url, List<String> targets ) {
		NaturalLanguageUnderstanding service = new NaturalLanguageUnderstanding(
				NaturalLanguageUnderstanding.VERSION_DATE_2017_02_27,	 "9b967bcc-3074-4ba2-ae02-5e197ae977f2",	"XMElVdlKc4nh");

		SentimentOptions sentiment = new SentimentOptions.Builder()
				  .targets(targets)
				  .build();

		//CategoriesOptions categoriesOptions = new CategoriesOptions();
		
		//ConceptsOptions conceptsOptions = new ConceptsOptions.Builder().limit(2).build();

		Features features = new Features.Builder()
				.sentiment(sentiment)
			//.keywords(keywordsOptions)
			//	.categories(categoriesOptions)
			//	.concepts(conceptsOptions)
				.build();

		AnalyzeOptions parameters = new AnalyzeOptions.Builder().url(url)
				.features(features)
				.build();

		AnalysisResults response = service
				.analyze(parameters)
				.execute();

		System.out.println(response);
		
		return response.toString();
	}
	
}