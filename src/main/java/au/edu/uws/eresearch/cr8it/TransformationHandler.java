/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package au.edu.uws.eresearch.cr8it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FilenameUtils;
import org.joda.time.LocalDate;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.support.MessageBuilder;

/**
 * This Spring Integration transformation handler takes the input file, extracts it into "./store" 
 * directory and read the "data/README.html" file and converts it into a json-ld string. It then 
 * converts the string into an upper-case string and then sets a few Spring Integration message headers.
 *
 * @author Lloyd Harischandra
 * @since 1.0
 */
public class TransformationHandler {

	/**
	 * Actual Spring Integration transformation handler.
	 *
	 * @param inputMessage Spring Integration input message
	 * @return New Spring Integration message with updated headers
	 */
	@Transformer
	public Message<String> handleFile(final Message<File> inputMessage) {

		final File inputFile = inputMessage.getPayload();
		final String filename = inputFile.getName();
		final String fileExtension = FilenameUtils.getExtension(filename);

		//populate this with json-ld data
		final String inputAsString;
		String finalString = "";
		
		if("zip".equals(fileExtension)){
			inputAsString = getJsonData(inputFile, FilenameUtils.getName(filename));
			
			try {
				finalString = getJsonMapping(inputAsString);
			} catch (IOException | ParseException e) {
				e.printStackTrace();
			}
			
			if(finalString.length() > 0){
				final Message<String> message = MessageBuilder.withPayload(finalString)
							.setHeader(FileHeaders.FILENAME,      FilenameUtils.getBaseName(filename) + ".json")
							.setHeader(FileHeaders.ORIGINAL_FILE, inputFile)
							.setHeader("file_size", finalString.length())
							.setHeader("file_extension", "json")
							.build();
		
				return message;
			}
			else
			{
				System.out.println("Empty json string.");
				return null;
			}
		}
		else
		{
			System.out.println("Invalid file format");
			return null;
		}
	}
	
	/**
	 * Return manifest.json as a string
	 * 
	 * @param archive
	 * @param archiveName
	 * @return json-ld string
	 */
	private String getJsonData(File archive, String archiveName){
		
		String extractedPath = extractZipArchive(archive, archiveName);
		String manifestFilePath = extractedPath + "/data/manifest.json";
		String jsonData = readFile(manifestFilePath);
		return jsonData;
	}
	
	private String getJsonMapping(String jsonString) throws IOException, ParseException{ 
		
		String template = readFile("./template.json");
		JSONParser parser = new JSONParser();
		JSONObject original = (JSONObject) parser.parse(template);
		JSONObject manifest = (JSONObject) parser.parse(jsonString);
		
		JSONObject dataObject = (JSONObject) original.get("data");
		JSONArray dataArray = (JSONArray) dataObject.get("data");
		
		JSONArray creators = (JSONArray) manifest.get("creators");
		JSONArray activities = (JSONArray) manifest.get("activities");
		JSONArray vfs = (JSONArray) manifest.get("vfs");
		
		LocalDate today = new LocalDate();
		int creatorIndex = 1;
		int grantIndex = 1;
		for(Object data : dataArray){
			
			((JSONObject) data).put("datasetId", jsonString.hashCode());
			JSONObject tfpackage =  (JSONObject) ((JSONObject) data).get("tfpackage");
			tfpackage.put("dc:created", today.toString());
			
			Object root = vfs.get(0);
			if(root != null){
				String crateName = (String) ((JSONObject) root).get("name");
				tfpackage.put("dc:title", crateName);
				tfpackage.put("title", crateName);
			}
			for(Object creator : creators){
				//TODO we might need to split the name into first and last name
				String name = (String) ((JSONObject) creator).get("name");
				tfpackage.put("dc:creator.foaf:Person." + creatorIndex + ".foaf:name", name);
				
				String identifier = (String) ((JSONObject) creator).get("identifier");
				tfpackage.put("dc:creator.foaf:Person." + creatorIndex + ".dc:identifier", identifier);
				
				creatorIndex++;
			}
			for(Object activity : activities){
				String identifier = (String) ((JSONObject) activity).get("identifier");
				tfpackage.put("foaf:fundedBy.vivo:Grant." + grantIndex + ".dc:identifier", identifier);
				
				String grantNumber = (String) ((JSONObject) activity).get("grant_number");
				tfpackage.put("foaf:fundedBy.vivo:Grant." + grantIndex + ".redbox:grantNumber", grantNumber);
				
				String title = (String) ((JSONObject) activity).get("title");
				String repositoryName = (String) ((JSONObject) activity).get("repository_name");
				tfpackage.put("foaf:fundedBy.vivo:Grant." + grantIndex + ".skos:prefLabel", "(" + repositoryName + ") " + title);
				
				grantIndex++;
			}
			
		}
		String updatedJson = original.toJSONString();
		return updatedJson;
		
	}
	
	private static String readFile(String path) 
	{
		byte[] encoded = null;
		try {
			encoded = Files.readAllBytes(Paths.get(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new String(encoded);
	}
	
	/**
	 * Extract the zip file to the output directory defined in the config
	 * 
	 * @param source 
	 * @param archiveName Name of the zip archive
	 * @return Path of the extracted file
	 */
	private String extractZipArchive(File source, String archiveName)
	{
		String destination = "store/" + archiveName;
		String password = "";
		try {
	         ZipFile zipFile = new ZipFile(source);
	         if (zipFile.isEncrypted()) {
	            zipFile.setPassword(password);
	         }
	         zipFile.extractAll(destination);
	    } catch (ZipException e) {
	        e.printStackTrace();
	    }
		return destination;
	}
	
}
