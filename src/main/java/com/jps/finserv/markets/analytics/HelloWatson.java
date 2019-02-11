package com.jps.finserv.markets.analytics;

import com.ibm.watson.developer_cloud.discovery.v1.model.environment.GetEnvironmentRequest;
import com.ibm.watson.developer_cloud.discovery.v1.model.environment.GetEnvironmentResponse;
import com.ibm.watson.developer_cloud.discovery.v1.model.environment.GetEnvironmentsRequest;
import com.ibm.watson.developer_cloud.discovery.v1.model.environment.GetEnvironmentsResponse;
import com.ibm.watson.developer_cloud.discovery.v1.model.query.QueryRequest;
import com.ibm.watson.developer_cloud.discovery.v1.model.query.QueryResponse;
import com.jps.finserv.markets.equities.Config;
import com.jps.finserv.markets.util.modReadFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.ibm.watson.developer_cloud.discovery.v1.Discovery;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HelloWatson {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(HelloWatson.class);

	public void run() {

		Discovery discovery = new Discovery(Config.WATSON_API_VERSION_DATE);

		discovery.setEndPoint(Config.WATSON_ENDPOINT);
		discovery.setUsernameAndPassword(Config.WATSON_USERNAME , Config.WATSON_PWD);
		listEnvironments(discovery);
	}

	/*
	 * TODO: Need to also create classes 
	 * for deleting and creating environments 
	 */

	private void listEnvironments(Discovery discovery){
		List<String> environmentIdList = new ArrayList<String>();

		GetEnvironmentsRequest getRequest = new GetEnvironmentsRequest.Builder().build();
		GetEnvironmentsResponse getResponse = discovery.getEnvironments(getRequest).execute();

		//logger.debug("\nListing Environments...");
		//logger.debug(getResponse.toString());

		/*
		 * TODO: Need to write a separate class to retrieve 
		 * basic information like name, pwd, env, coll etc... 
		 * That class will either have the information hard-coded (for now)
		 * or better retrieve it based on basic queries.
		 * 
		      String userName = System.getenv("DISCOVERY_USERNAME");
		      String password = System.getenv("DISCOVERY_PASSWORD");
		      String collectionId = System.getenv("DISCOVERY_COLLECTION_ID");
		      String environmentId = System.getenv("DISCOVERY_ENVIRONMENT_ID");  
		 */

		// TODO: Retrieve this using the call return and not hard-code it here. 
		String environmentId = "c7972f49-58df-44db-80b7-21053481e39b";
		String collectionId = "8400196c-bc8b-4b8e-9e3b-2979d4b75794";

		environmentIdList.add(environmentId);
		getEnvironment(discovery, environmentIdList);
		//queryWatson(discovery, environmentId, collectionId);
		queryWatson(discovery, Config.WATSON_NEWS_ENV_ID, Config.WATSON_NEWS_ENV_COLLECTION);
	}

	private void getEnvironment(Discovery discovery, List<String> environmentIdList){

		for (String environmentID: environmentIdList) {
			GetEnvironmentRequest getRequest = new GetEnvironmentRequest.Builder(environmentID).build();
			GetEnvironmentResponse getResponse = discovery.getEnvironment(getRequest).execute();

			//logger.debug("\nListing details for Environment with ID \""+environmentID+"");
			//logger.info(getResponse.toString());
		}
	}

	private void queryWatson(Discovery discovery, String environmentId, String collectionId){
		List<String> environmentIdList = new ArrayList<String>();

		try{
			JsonNode retJsonNode = JsonNodeFactory.instance.objectNode();
			String workingDir = System.getProperty("user.dir");
			String filePath = workingDir + File.separator + "JSON" + File.separator + "query.json";

			String jsonPost = modReadFile.readFile(filePath);
			ObjectMapper mapper = new ObjectMapper();
			retJsonNode  = mapper.readTree(jsonPost);

			//logger.info(retJsonNode.toString());

			QueryRequest.Builder queryBuilder = new QueryRequest.Builder(environmentId, collectionId);
			
			StringBuilder sb = new StringBuilder();
			//String query = "enriched_text.entities.text:\"pfe\",enriched_text.entities.text:\"pfizer\"";
			String query = "enriched_text.entities.text:\"pfe\",enriched_text.entities.text:\"pfizer\"";
			
			//String urldecoded = URLDecoder.decode(retJsonNode.toString(), "UTF-8");
			//String urldecoded = URLDecoder.decode(url.toString(), "UTF-8");
			
			sb.append(query);
			//queryBuilder.query(retJsonNode.toString());
			queryBuilder.query(sb.toString());
			
			QueryResponse queryResponse = discovery.query(queryBuilder.build()).execute();
			logger.info("\nListing Query Response...");
			logger.info(queryResponse.toString());
			
		}
		catch (JsonProcessingException e){
			logger.error("Exception during JSON Parsing for Association Response: "+e.getMessage());
			e.printStackTrace();
		}

		catch (IOException e){
			logger.error("IOException during JSON Parsing for Association Response: "+e.getMessage());
			e.printStackTrace();
		}
	}
}



























