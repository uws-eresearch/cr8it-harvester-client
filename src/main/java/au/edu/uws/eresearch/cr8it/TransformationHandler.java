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
import java.util.Locale;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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
	public Message<byte[]> handleFile(final Message<File> inputMessage) {

		final File inputFile = inputMessage.getPayload();
		final String filename = inputFile.getName();
		final String fileExtension = FilenameUtils.getExtension(filename);

		//populate this with json-ld data
		final String inputAsString;
		
		if("zip".equals(fileExtension)){
			//get json-ld data
			
			String inputAsJLString = getJsonData(inputFile, FilenameUtils.getName(filename));
			
			//TODO inputAsString data format is json-ld. We might have to convert it to json data that json-harvester-client
			//undrestands
			
			inputAsString = getJsonMapping(inputAsJLString);
			
			try {
				byte[] ba = FileUtils.readFileToByteArray(inputFile);
			
			
				if(ba.length > 0){
					final Message<byte[]> message = MessageBuilder.withPayload(ba)
								.setHeader(FileHeaders.FILENAME,      filename)
								.setHeader(FileHeaders.ORIGINAL_FILE, inputFile)
								.setHeader("file_size", inputFile.length())
								.setHeader("file_extension", "zip")
								.build();
			
					return message;
				}
				else
				{
					System.out.println("Empty json string.");
					return null;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		else
		{
			System.out.println("Invalid file format");
			return null;
		}
	}
	
	public String getJsonMapping(String json_ld){
		
		//If we get json string as an object, we can extract data easily.
		String jsonString ="{\"type\": \"DatasetJson\","
				+ "\"data\": {"
				+ "\"data\": ["
				+ "{"
				+ "\"varMap\": {"
				+ "\"file.path\": \"${fascinator.home}/packages/<oid>.tfpackage\""
				+ "},"
				+ "\"tfpackage\": {"
				+ "\"redbox:embargo.redbox:isEmbargoed\": \"\","
				+ "\"redbox:embargo.dc:date\": \"\","
				+ "\"dc:created\": \"2013-12-06\","
				+ "\"dc:creator.foaf:Person.1.foaf:givenName\": \"\","
				+ "\"dc:creator.foaf:Person.1.foaf:familyName\": \"\","
				+ "\"dc:creator.foaf:Person.1.foaf:name\": \"\","
				+ "\"dc:title\": \"Lloyd Test\","
				+ "\"title\": \"Just Lloyd Test\","
				+ "\"dc:description\": \"Description for the test\","
				+ "\"description\": \"Just Description for the test\","
				+ "\"metaList\": ["
				+ "\"dc:title\","
				+ "\"dc:type.rdf:PlainLiteral\","
				+ "\"dc:type.skos:prefLabel\","
				+ "\"dc:created\","
				+ "\"dc:modified\","
				+ "\"dc:description\","
				+ "\"xmlns:dc\","
				+ "\"xmlns:foaf\","
				+ "\"xmlns:anzsrc\""
				+ "]},"
				+ "\"datasetId\": \"someId\","
				+ "\"owner\": \"admin\","
				+ "\"attachmentDestination\": {"
				+ "\"tfpackage\": ["
				+ "\"<oid>.tfpackage\","
				+ "\"metadata.json\","
				+ "\"$file.path\""
				+ "],"
				+ "\"workflow.metadata\": ["
				+ "\"workflow.metadata\""
				+ "]"
				+ "},"
				+ "\"attachmentList\": ["
				+ "\"tfpackage\","
				+ "\"workflow.metadata\""
				+ "],"
				+ "\"customProperties\": ["
				+ "\"file.path\""
				+ "],"
				+ "\"workflow.metadata\": {"
				+ "\"id\": \"dataset\","
				+ "\"formData\": {"
				+ "\"title\": \"\","
				+ "\"description\": \"\""
				+ "},"
				+ "\"pageTitle\": \"Metadata Record\","
				+ "\"label\": \"Metadata Review\","
				+ "\"step\": \"metadata-review\""
				+ "}}"
				+ "]}"
				+ "}";
		
		return jsonString;
	}
	
	/**
	 * Should return a json data out of README.html
	 * 
	 * @param archive
	 * @param archiveName
	 * @return json-ld string
	 */
	private String getJsonData(File archive, String archiveName){
		
		//extract zip file and return location
		String extractedPath = extractZipArchive(archive, archiveName);
		String readmeFilePath = extractedPath + "/data/README.html";
		File readmeFile = new File(readmeFilePath);
		String jsonData = "";
		if(readmeFile.exists()){
			try
	        {
				//execute python process on README.html file at the location
	            Runtime r = Runtime.getRuntime();
	            String command = "python /opt/RDFLib/pyrdfa3/scripts/localRDFa.py -j " + readmeFilePath;
	            Process p = r.exec(command);
	            	            
	            jsonData = IOUtils.toString(p.getInputStream());
	            
	        }
	        catch (Exception e)
	        {
		        String cause = e.getMessage();
		        if (cause.equals("python: not found"))
		        {
		        	System.out.println("No python interpreter found.");
		        }
	        }
		}
		else{
			System.out.println("No README file. Return empty string");
		}
		return jsonData;
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
