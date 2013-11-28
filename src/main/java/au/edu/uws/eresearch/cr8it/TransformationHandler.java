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
import java.util.Locale;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

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
	public Message<String> handleFile(final Message<File> inputMessage) {

		final File inputFile = inputMessage.getPayload();
		final String filename = inputFile.getName();
		final String fileExtension = FilenameUtils.getExtension(filename);

		//populate this with json-ld data
		final String inputAsString;
		
		if("zip".equals(fileExtension)){
			//get json-ld data
			inputAsString = getJsonData(inputFile, FilenameUtils.getName(filename));
			
			//TODO inputAsString data format is json-ld. We might have to convert it to json data that json-harvester-client
			//undrestands
			
			if(inputAsString.length() > 0){
				final Message<String> message = MessageBuilder.withPayload(inputAsString.toUpperCase(Locale.ENGLISH))
							.setHeader(FileHeaders.FILENAME,      FilenameUtils.getBaseName(filename) + ".json")
							.setHeader(FileHeaders.ORIGINAL_FILE, inputFile)
							.setHeader("file_size", inputAsString.length())
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
