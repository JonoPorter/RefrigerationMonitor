/**
 *  Hubitat Import URL: https://raw.githubusercontent.com/JonoPorter/RefrigerationMonitor/master/RefrigerationMonitor_Parent.groovy
 */

/**
 *  Alert on Power Consumption
 *
 *  Copyright 2019 Jonathan Porter, Kevin Tierney, C Steele
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

//Better Laundry Monitor was used as the base for this code

	public static String version()      {  return "v1.4.4"  }


definition(
    name: "Refrigeration Monitor",
    namespace: "jonoporter",
    author: "Jonathan Porter, Kevin Tierney, CSteele",
    description: "Using a switch with powerMonitor capability, monitor the Freezer or Refrigerator power usage and inform if they are out of norm.",
    category: "Green Living",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    )


preferences {
     page name: "mainPage", title: "", install: true, uninstall: true // ,submitOnChange: true      
} 


def mainPage() {
	dynamicPage(name: "mainPage") {
		section {    
			paragraph title: "<Refrigeration Monitor",
			"<b>This parent app is a container for all:</b><br> Refrigeration Monitor - Power Switch child apps"
		}
      	section (){app(name: "BlMpSw", appName: "Refrigeration Monitor - Power Switch", namespace: "jonoporter", title: "New Refrigeration Monitor - Power Switch App", multiple: true)}    
      	  
      	section (title: "<b>Name/Rename</b>") {label title: "Enter a name for this parent app (optional)", required: false}
	
		section ("Other preferences") {
			input "debugOutput",   "bool", title: "<b>Enable debug logging?</b>", defaultValue: true
			input "descTextEnable","bool", title: "<b>Enable descriptionText logging?</b>", defaultValue: true
		}
      	display()
	} 
}


def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}


def updated() {
	log.debug "Updated with settings: ${settings}"
	unschedule()
	unsubscribe()
	if (debugOutput) runIn(1800,logsOff)
	initialize()
}


def initialize() {
	log.info "There are ${childApps.size()} child smartapps"
	childApps.each {child ->
		child.setDebug(debugOutput, descTextEnable)
		log.info "Child app: ${child.label}"
	}
}


def logsOff() {
    log.warn "debug logging disabled..."
    app?.updateSetting("debugOutput",[value:"false",type:"bool"])
}


def display() {
	section{
		paragraph "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
		paragraph "<div style='color:#1A77C9;text-align:center;font-weight:small;font-size:9px'>Developed by: Kevin Tierney, ChrisUthe, C Steele<br/>Version Status: $state.Status<br>Current Version: ${version()} -  ${thisCopyright}</div>"
	}
}
  
  

def getThisCopyright(){"&copy; 2019 Jonathan Porter"}