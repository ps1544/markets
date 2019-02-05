package com.jps.finserv.markets.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class modReadFile {

	//String fileName = null;
	PrintWriter p=null;
	Object[] tempStrArr = null;

	public static String readFile(String fileName){
		//fileName = fileName;
		String inputJSON = "";
		String inputStr;

		try{
			File f = new File(fileName.trim());
			InputStream stream = new FileInputStream(f);

			BufferedReader streamReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			//BufferedReader bReader = new BufferedReader(new FileReader(fileName));

			while ((inputStr = streamReader.readLine()) != null)
				inputJSON = inputJSON.concat(inputStr.trim());
			streamReader.close();
		}
		catch(Exception e){
			System.out.println("Cannot read from file " + fileName + "\n");
			System.out.println("Exception: " + e.toString());
			//System.exit(1);
		}
		return inputJSON;	
	}

/**
 * Reads a file in current directory and returns it in a list of lines. Very useful for CSV parsing.
 * @param fileName
 * @return
 */
	public static List<String> readFileIntoListOfStrings(String fileName){
		//fileName = fileName;
		String inputStr;

		List<String> returnList = new ArrayList<String>();
		try{
			File f = new File(fileName.trim());
			InputStream stream = new FileInputStream(f);

			BufferedReader streamReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			//BufferedReader bReader = new BufferedReader(new FileReader(fileName));

			while ((inputStr = streamReader.readLine()) != null)
					returnList.add(inputStr);
			streamReader.close();
		}
		catch(Exception e){
			System.out.println("Cannot read from file " + fileName + "\n");
			System.out.println("Exception: " + e.toString());
			//System.exit(1);
		}
		return returnList;	
	}

	/**
	 * Reads a file from the given directory path and returns it in a list of lines. Very useful for CSV parsing.
	 * @param fileName
	 * @return
	 */

	public static List<String> readFileIntoListOfStringsDirectory(String dirPath, String fileName){
		//fileName = fileName;
		File dir = new File(dirPath.trim());
		String inputStr;
		
		List<String> returnList = new ArrayList<String>();
		try{
			if (dir.exists()){
				String path = dir.getAbsolutePath().concat(File.pathSeparator).concat(fileName.trim());
				//File f = new File(path);
				File f = new File(dir, fileName.trim());
				InputStream stream = new FileInputStream(f);
	
				BufferedReader streamReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
				//BufferedReader bReader = new BufferedReader(new FileReader(fileName));
	
				while ((inputStr = streamReader.readLine()) != null)
						returnList.add(inputStr);
				streamReader.close();
			}
		}
		catch(Exception e){
			System.out.println("Cannot read from file " + fileName + "\n");
			System.out.println("Exception: " + e.toString());
			//System.exit(1);
		}
		return returnList;	
	}

}