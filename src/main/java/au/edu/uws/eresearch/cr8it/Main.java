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

import groovy.util.ConfigObject;

import java.io.File;
import java.util.Map;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import au.com.redboxresearchdata.util.config.Config;


/**
 * Starts the Spring Context and will initialize the Spring Integration routes.
 *
 * @author Lloyd Harischandra
 * @since 1.0
 *
 */
public final class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class);

	private Main() { }

	/**
	 * Load the Spring Integration Application Context
	 *
	 * @param args - command line arguments
	 */
	public static void main(final String... args) {

		String contextFilePath = "spring-integration-context.xml";
		String configFilePath = "config/config-file.groovy";
		String environment = System.getProperty("environment");
		
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("\n========================================================="
					  + "\n                                                         "
					  + "\n          Welcome to C8it Integration!                 "
					  + "\n                                                         "
					  + "\n    For more information please visit:                   "
					  + "\n    http://www.springsource.org/spring-integration       "
					  + "\n                                                         "
					  + "\n=========================================================" );
		}

		ConfigObject config = Config.getConfig(environment, configFilePath);
		Map configMap = config.flatten();
		System.setProperty("environment", environment);
		System.setProperty("cr8it.client.config.file", (String) configMap.get("file.runtimePath"));
		
		//final AbstractApplicationContext context =
				//new ClassPathXmlApplicationContext("classpath:META-INF/spring/integration/*-context.xml");
		
		String absContextPath = "config/integration/" + contextFilePath;
		File contextFile = new File(absContextPath);
		final AbstractApplicationContext context;
		if (!contextFile.exists()) {
			absContextPath = "classpath:"+absContextPath;
			context =
					new ClassPathXmlApplicationContext(absContextPath);
		} else {
			absContextPath = "file:" + absContextPath; 
			context =
					new FileSystemXmlApplicationContext(absContextPath);
		}

		context.registerShutdownHook();

		SpringIntegrationUtils.displayDirectories(context);

		final Scanner scanner = new Scanner(System.in);

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("\n========================================================="
					  + "\n                                                         "
					  + "\n    Please press 'q + Enter' to quit the application.    "
					  + "\n                                                         "
					  + "\n=========================================================" );
		}

		while (!scanner.hasNext("q")) {
			//Do nothing unless user presses 'q' to quit.
		}

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Exiting application...bye.");
		}

		System.exit(0);

	}
}
