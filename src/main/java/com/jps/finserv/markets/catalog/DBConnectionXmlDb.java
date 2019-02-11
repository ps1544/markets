package com.jps.finserv.markets.catalog;

import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;
import org.xmldb.api.*;
import org.exist.xmldb.EXistResource;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBConnectionXmlDb {

	private final Logger logger = (Logger) LoggerFactory.getLogger(DBConnectionXmlDb.class);
	final String driver = "org.exist.xmldb.DatabaseImpl";

	//private static String URI = "xmldb:exist://localhost:8080/exist/xmlrpc";
	private static String URI = "xmldb:exist://localhost:8080/exist/xmlrpc/db";
	private static final String existUserName = "admin";
	private static final String existUserPassword = "$ys8dmin";
	
	
	/**
	 * collection name of the collection to access
	 * strFilePath name of the file to read and store in the collection
	 */
	public void testConn(String collection, String strFilePath) throws Exception {

		// initialize database driver
		Class cl = Class.forName(driver);
		Database database = (Database) cl.newInstance();
		database.setProperty("create-database", "true");
		DatabaseManager.registerDatabase(database);
		Collection col = null;
		XMLResource res = null;
		try { 
			col = getOrCreateCollection(collection);
			// create new XMLResource; an id will be assigned to the new 
			res = (XMLResource)col.createResource(null, "XMLResource");
			File f = new File(strFilePath);
			if(!f.canRead()) {
				System.out.println("cannot read file " + strFilePath);
				return;
			}

			res.setContent(f);
			logger.info("storing document " + res.getId() + "...");
			col.storeResource(res);
		} finally {
			//dont forget to cleanup
			if(res != null) {
				try { ((EXistResource)res).freeResources(); } catch(XMLDBException xe) {xe.printStackTrace();}
			}

			if(col != null) {
				try { col.close(); } catch(XMLDBException xe) {xe.printStackTrace();}
			}
		}
	}

	private static Collection getOrCreateCollection(String collectionUri) throws XMLDBException {
		return getOrCreateCollection(collectionUri, 0);
	}

	private static Collection getOrCreateCollection(String collectionUri, int pathSegmentOffset) throws XMLDBException {

		Collection col = DatabaseManager.getCollection(URI + collectionUri, existUserName, existUserPassword);
		if(col == null) {
			if(collectionUri.startsWith("/")) {
				collectionUri = collectionUri.substring(1);
			}
			String pathSegments[] = collectionUri.split("/");
			if(pathSegments.length > 0) {
				StringBuilder path = new StringBuilder();
				for(int i = 0; i <= pathSegmentOffset; i++) {
					path.append("/" + pathSegments[i]);
				}
				Collection start = DatabaseManager.getCollection(URI + path, existUserName, existUserPassword);
				if(start == null) {
					//collection does not exist, so create
					String parentPath = path.substring(0, path.lastIndexOf("/"));
					Collection parent = DatabaseManager.getCollection(URI +parentPath, existUserName, existUserPassword);
					CollectionManagementService mgt = (CollectionManagementService) parent.getService("CollectionManagementService", "1.0");
					col = mgt.createCollection(pathSegments[pathSegmentOffset]);
					col.close();
					parent.close();
				} else {
					start.close();
				}
			}
			return getOrCreateCollection(collectionUri, ++pathSegmentOffset);
		} else {
			return col;
		}
	}

	
}

