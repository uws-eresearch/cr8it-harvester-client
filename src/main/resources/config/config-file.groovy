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
					directory = "json-harvester-client/input"
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