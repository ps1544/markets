package com.jps.finserv.markets.callreport;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

import org.xbrlapi.Concept;
import org.xbrlapi.Context;
import org.xbrlapi.ExtendedLink;
import org.xbrlapi.FootnoteResource;
import org.xbrlapi.Fragment;
import org.xbrlapi.Instance;
import org.xbrlapi.Item;
import org.xbrlapi.Resource;
import org.xbrlapi.Stub;
import org.xbrlapi.Unit;
import org.xbrlapi.cache.CacheImpl;
import org.xbrlapi.data.Store;
//import org.xbrlapi.data.bdbxml.StoreImpl;
import org.xbrlapi.data.exist.StoreImpl;
import org.xbrlapi.grabber.Grabber;
import org.xbrlapi.grabber.SecGrabberImpl;
import org.xbrlapi.loader.Loader;
import org.xbrlapi.loader.discoverer.Discoverer;
import org.xbrlapi.networks.AnalyserImpl;
import org.xbrlapi.sax.EntityResolver;
import org.xbrlapi.sax.EntityResolverImpl;
import org.xbrlapi.utilities.Constants;
import org.xbrlapi.utilities.XBRLException;
import org.xbrlapi.xdt.LoaderImpl;
import org.xbrlapi.xlink.XLinkProcessor;
import org.xbrlapi.xlink.XLinkProcessorImpl;
import org.xbrlapi.xlink.handler.XBRLCustomLinkRecogniserImpl;
import org.xbrlapi.xlink.handler.XBRLXLinkHandlerImpl;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XbrlIntegrationImpl {

	private final Logger logger = (Logger) LoggerFactory.getLogger(XbrlIntegrationImpl.class);
	private static Store store = null;
	final String driver = "org.exist.xmldb.DatabaseImpl";

	private static final String cache = 	"C:/Users/pshar/Dropbox/workspace/StockData/CallReportSample";

	public void LoadValidate(){
		
		Class cl;
		Database database;
	
		try {

			cl = Class.forName(driver);
			database = (Database) cl.newInstance();
			database.setProperty("create-database", "true");

			List<URI> inputs = new LinkedList<URI>();

			inputs.add(new URI("https://www.sec.gov/Archives/edgar/data/886982/000119312517331475/gs-20170930.xml"));
			inputs.add(new URI("https://www.sec.gov/Archives/edgar/data/886982/000119312517331475/gs-20170930.xsd"));
			inputs.add(new URI("https://www.sec.gov/Archives/edgar/data/886982/000119312517331475/gs-20170930_cal.xml"));
			inputs.add(new URI("https://www.sec.gov/Archives/edgar/data/886982/000119312517331475/gs-20170930_def.xml"));
			inputs.add(new URI("https://www.sec.gov/Archives/edgar/data/886982/000119312517331475/gs-20170930_lab.xml"));
			inputs.add(new URI("https://www.sec.gov/Archives/edgar/data/886982/000119312517331475/gs-20170930_pre.xml"));

			// RSS feed for all entries: See ""https://sourceforge.net/p/xbrlapi/org_xbrlapi/ci/master/tree/org.xbrlapi/module-examples/src/main/java/org/xbrlapi/data/bdbxml/examples/load/LoadAllSECFilings.java#l100"
			//inputs.add(new URI("https://www.sec.gov/Archives/edgar/xbrlrss.all.xml"));
			// Make sure that the taxonomy cache exists
			File fileCache = new File(cache);
			if (!fileCache.exists()) 
			{
				logger.error("The document cache directory does not exist: \"" + fileCache.toString()+ "\"");
			}

			// Set DB connection 
			store = new StoreImpl("localhost", "8080", "exist/xmlrpc/db", "admin", "$ys8dmin", "/", "xbrl");

			//Set up the data loader (does the parsing and data discovery)
			Loader loader;

			loader = createLoader(store, cache);

			// Do the document discovery
			int size = store.getSize();
			logger.info("The Store size is: "+size+".");
			// Load the instance data
			loader.discover(inputs);

			// Check that all documents were loaded OK.
			List<Stub> stubs = store.getStubs();
			if (! stubs.isEmpty()) {
				for (Stub stub: stubs) {
					logger.info(stub.getResourceURI() + ": " + stub.getReason());
				}
				logger.error("Some documents were not loaded.");
				// Clean up the data store and exit
				cleanup(store);
			}
		}
			catch (XBRLException e) {e.getMessage(); e.printStackTrace();}
			catch (ClassNotFoundException e2) {e2.printStackTrace();	}
			catch (XMLDBException e) {e.printStackTrace();}
			catch (InstantiationException | IllegalAccessException e) {e.printStackTrace();}
			catch (URISyntaxException e1) {e1.printStackTrace();}}

			/**
			 * @param store The store to use for the loader.
			 * @param cache The root directory of the document cache.
			 * @return the loader to use for loading the instance and its DTS
			 * @throws XBRLException if the loader cannot be initialised.
			 */
			private static Loader createLoader(Store store, String cache) throws XBRLException {
				XBRLXLinkHandlerImpl xlinkHandler = new XBRLXLinkHandlerImpl();
				XBRLCustomLinkRecogniserImpl clr = new XBRLCustomLinkRecogniserImpl(); 
				XLinkProcessor xlinkProcessor = new XLinkProcessorImpl(xlinkHandler ,clr);

				File cacheFile = new File(cache);

				// Rivet errors in the SEC XBRL data require these URI remappings to prevent discovery process from breaking.
				HashMap<URI,URI> map = new HashMap<URI,URI>();
				try {
					map.put(new URI("http://www.xbrl.org/2003/linkbase/xbrl-instance-2003-12-31.xsd"),new URI("http://www.xbrl.org/2003/xbrl-instance-2003-12-31.xsd"));
					map.put(new URI("http://www.xbrl.org/2003/instance/xbrl-instance-2003-12-31.xsd"),new URI("http://www.xbrl.org/2003/xbrl-instance-2003-12-31.xsd"));
					map.put(new URI("http://www.xbrl.org/2003/linkbase/xbrl-linkbase-2003-12-31.xsd"),new URI("http://www.xbrl.org/2003/xbrl-linkbase-2003-12-31.xsd"));
					map.put(new URI("http://www.xbrl.org/2003/instance/xbrl-linkbase-2003-12-31.xsd"),new URI("http://www.xbrl.org/2003/xbrl-linkbase-2003-12-31.xsd"));
					map.put(new URI("http://www.xbrl.org/2003/instance/xl-2003-12-31.xsd"),new URI("http://www.xbrl.org/2003/xl-2003-12-31.xsd"));
					map.put(new URI("http://www.xbrl.org/2003/linkbase/xl-2003-12-31.xsd"),new URI("http://www.xbrl.org/2003/xl-2003-12-31.xsd"));
					map.put(new URI("http://www.xbrl.org/2003/instance/xlink-2003-12-31.xsd"),new URI("http://www.xbrl.org/2003/xlink-2003-12-31.xsd"));
					map.put(new URI("http://www.xbrl.org/2003/linkbase/xlink-2003-12-31.xsd"),new URI("http://www.xbrl.org/2003/xlink-2003-12-31.xsd"));
				} catch (URISyntaxException e) {
					throw new XBRLException("URI syntax exception",e);
				}
				EntityResolver entityResolver = new EntityResolverImpl(cacheFile,map);      

				Loader myLoader = new LoaderImpl(store,xlinkProcessor, entityResolver);
				myLoader.setCache(new CacheImpl(cacheFile));
				myLoader.setEntityResolver(entityResolver);
				xlinkHandler.setLoader(myLoader);
				return myLoader;
			}


			/**
			 * Report the information about a concept in the presentation heirarchy
			 * @param indent The indent to use for reporting the fragment
			 * @param fragment The fragment to report
			 * @param linkRole The linkrole of the network to use
			 * @throws XBRLExceptions
			 */
			private static void reportNode(String indent, Fragment fragment, String linkRole) throws XBRLException {
				Concept concept = (Concept) fragment;
				System.out.println(indent + concept.getTargetNamespace() + ":" + concept.getName());
				List<Fragment> children = store.getTargets(concept.getIndex(),linkRole,Constants.PresentationArcrole);
				if (children.size() > 0) {
					for (Fragment child: children) {
						reportNode(indent + " ", child,linkRole);
					}  
				}
			}

			private static void reportInstance(Instance instance) throws XBRLException {
				List<Item> items = instance.getChildItems();
				System.out.println("Top level items in the instance.");
				for (Item item: items) {
					System.out.println(item.getLocalname() + " " + item.getContextId());
				}

				List<Context> contexts = instance.getContexts();
				System.out.println("Contexts in the instance.");
				for (Context context: contexts) {
					System.out.println("Context ID " + context.getId());
				}

				List<Unit> units = instance.getUnits();
				System.out.println("Units in the instance.");
				for (Unit unit: units) {
					System.out.println("Unit ID " + unit.getId());
				}

				List<ExtendedLink> links = instance.getFootnoteLinks();
				System.out.println("Footnote links in the instance.");
				for (ExtendedLink link: links) {            
					List<Resource> resources = link.getResources();
					for (Resource resource: resources) {
						FootnoteResource fnr = (FootnoteResource) resource;
						System.out.println("Footnote resource: " + fnr.getDataRootElement().getTextContent());
					}
				}

			}


			/**
			 * Helper method to clean up and shut down the data store.
			 * @param store the store for the XBRL data.
			 * @throws XBRLException if the store cannot be closed. 
			 */
			private static void cleanup(Store store) throws XBRLException {
				store.close();
			}    

			/*
			 * Sample program for Arelle based XBRL reading. 
			 * Arelle can't do much else w/ Java. Must use UI I think beyond this. 
			 * Requires CLI to run on port 8080. Without that will return connection refused. 
			 */
			public void LoadValidateUsingArelle (){

				try{

					String restAPIstr = "http://localhost:8080/rest/xbrl/" 
							+ 	"C:/Users/pshar/Dropbox/workspace/StockData/CallReportSample/Call_Cert3510_093017.XBRL" 
							+ "/validation/xbrl?media=text";
					URL url = new URL(restAPIstr);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					if (conn.getResponseCode() != 200) {
						throw new IOException(conn.getResponseMessage());
					}
					// Buffer the result into a string
					BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					StringBuilder sb = new StringBuilder();
					String line = "";;
					while (rd.readLine() != null) {
						sb.append(line);
						System.out.println(line);
					}
					rd.close();
					conn.disconnect();
				}
				catch(MalformedURLException e){logger.error("Failed to open URL: ");	e.getMessage();}
				catch(IOException e){logger.error("I/O Exception: "+e.getMessage());} 


			}

		}