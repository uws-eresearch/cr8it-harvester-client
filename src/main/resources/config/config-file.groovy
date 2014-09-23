/**
 * Cr8it file harvest Configuration File
 * ----------------------------------------------------------------------
 * PROJECT: Cr8it File Harvester Client
 * ----------------------------------------------------------------------
 *
 * @author Lloyd Harischandra
 *
 */
// Environment specific config below...
environments {
	development {
		file {
			runtimePath = "src/test/resources/config/generated/config-file.groovy"
			customPath = "src/test/resources/config/config-file.groovy"
		}
		harvest {
			directory = "input"
			pollRate = "5000"
			queueCapacity = "10"
			output {
				json {
					directory = "target/output/json"
					deletesource = "true"
				}
				other {
					directory = "target/output/other"
					deletesource = "true"
				}
			}
		}
	}
	production {
		file {
			runtimePath = "config/generated/config-file.groovy"
			customPath = "config/config-file.groovy"
		}
		harvest {
			directory = "input"
			pollRate = "5000"
			queueCapacity = "10"
			output {
				json {
					directory = "/home/lloyd/UWS/REDBOX/json-harvester-client-1.0.0.BUILD-20140618.232015-49-bin/output/json"
					deletesource = "true"
				}
				other {
					directory = "output/other"
					deletesource = "true"
				}
			}
		}
	}
}