/**
 *  Hubitat Import URL: https://raw.githubusercontent.com/JonoPorter/RefrigerationMonitor/master/RefrigerationMonitor_Child.groovy
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
public static String version()      {  return "v1.0"  }

//Better Laundry Monitor was used as the base for this code

import groovy.time.*

definition(
	name: "Refrigeration Monitor - Power Switch",
	namespace: "jonoporter",
	author: "Jonathan Porter, Kevin Tierney, ChrisUthe, CSteele",
	description: "Child: powerMonitor capability, monitor the Freezer or Refrigerator power usage and inform if they are out of norm.",
	category: "Green Living",
	    
	parent: "jonoporter:Refrigeration Monitor",
	
	iconUrl: "",
	iconX2Url: "",
	iconX3Url: ""
)


preferences {
	page (name: "mainPage")
	page (name: "sensorPage")
	page (name: "thresholdPage")
	page (name: "informPage")
}
//<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>

def mainPage() {
	dynamicPage(name: "mainPage", install: true, uninstall: true) {
		updateMyLabel()
		section("<h2>${app.label ?: app.name}</h2>"){
			if (!atomicState.isPaused) {
				input(name: "pauseButton", type: "button", title: "Pause", backgroundColor: "Green", textColor: "white", submitOnChange: true)
			} else {
				input(name: "resumeButton", type: "button", title: "Resume", backgroundColor: "Crimson", textColor: "white", submitOnChange: true)
			}
		}
		section
		{
			href "sensorPage", title: "Sensors", description: "Sensors to be monitored", state: selectOk?.sensorPage ? "complete" : null
			href "thresholdPage", title: "Thresholds", description: "Thresholds to be monitored", state: selectOk?.thresholdPage ? "complete" : null
			href "informPage", title: "Inform", description: "Who and what to Inform", state: selectOk?.informPage ? "complete" : null
		}
		section (title: "<b>Name/Rename</b>") {
			input( name:"appTitle", title: "This child app's Name", type: "text", required: true, submitOnChange: true, defaultValue:"New - Monitor")
			if (!appTitle) {
				app.updateLabel(app.Name)
			}
		}
		display()
	}
}
def appButtonHandler(btn) {
    switch(btn) {
        case "pauseButton":
			atomicState.isPaused = true
            break
		case "resumeButton":
			atomicState.isPaused = false
			break
    }
}

def sensorPage() {
	dynamicPage(name: "sensorPage") {
		section ("<b>Monitor this device and warn if the power usage is out of bounds.</b>") {
			input "pwrMeter", "capability.powerMeter", title: "Power Meter" , multiple: false, required: false, defaultValue: null
		}
	 
	}
}


def thresholdPage() {
	dynamicPage(name: "thresholdPage") {
		section ("<b>Power Thresholds</b>", hidden: false, hideable: true) {
			input "maxPowerThreshold", "decimal", title: "The Maximum power (Watts) allowed to be drawn before a instant warning. ", defaultValue: "1000", required: true
			input "desiredPowerThreshold", "decimal", title: "The desired power (Watts) for the be using most often. Best to have it a little below the desired.", defaultValue: "100", required: true
			input "lowPowerDelay", "number", title: "How long (Minutes) before warning the user when the power is below the desired", defaultValue: "120", required: true
		}
	}
}


def informPage() {
	dynamicPage(name: "informPage") {
		section ("<b>Send this message</b>", hidden: false, hideable: true) {
			input "highPowerMessage", "text", title: "Notification message High Power Usage", description: "Refrigerator is drawing too much power!", required: true
			input "lowPowerMessage", "text", title: "Notification message Low Power Usage", description: "Refrigerator is drawing too little power!", required: true
		}
		section (title: "<b>Using this Notification Method</b>", hidden: false, hideable: true) {
			input "textNotification", "capability.notification", title: "Send Via: (Notification)", multiple: true, required: false
			input "speechOut", "capability.speechSynthesis", title:"Speak Via: (Speech Synthesis)", multiple: true, required: false
			input "speechEchoAnnouncement", "bool", title:"Speak Via 'Echo Speaks' Play Announcement All (only need one echo device in above list)", defaultValue: false, required: false
			input "player", "capability.musicPlayer", title:"Speak Via: (Music Player -> TTS)", multiple: true, required: false
		}
		section ("<b>Choose Additional Devices</b>") {
		  	input "switchList", "capability.switch", title: "Which Switches?", description: "Switches to follow the active state", multiple: true, hideWhenEmpty: false, required: false             
		}
	}
}


def getSelectOk()
{
	def status =
	[
		sensorPage: pwrMeter,
		thresholdPage: maxPowerThreshold ?: desiredPowerThreshold ?: desiredPowerThreshold ,
		informPage: messageStart?.size() ?: message?.size()
	]
	status << [all: status.sensorPage ?: status.thresholdPage ?: status.informPage]
}
def delayedPowerCheck()
{
	powerHandler(null)
}

def powerHandler(evt) {
	def latestPower = pwrMeter.currentValue("power")	
	if (debugOutput) log.debug "Power: ${latestPower}W, State: ${atomicState.isLowPower}, thresholds: ${desiredPowerThreshold} ${maxPowerThreshold} ${lowPowerDelay}"
	if(latestPower>desiredPowerThreshold)
	{
		if(atomicState.sentNotification) { 
			atomicState.sentNotification = false
			if(debugOutput) log.debug "System is back online."
			if(switchList ) { switchList*.off() }
		}
		if(atomicState.isScheduled)
		{
			atomicState.isScheduled = false
			if(debugOutput) log.debug "unschedule delayed power check"
			unschedule(delayedPowerCheck)
		}
		atomicState.isLowPower = false
		atomicState.lastPowerUsage = new Date().getTime()
	}
	else
	{
		atomicState.isLowPower = true;
		def delayedTime =1000*60*lowPowerDelay + atomicState.lastPowerUsage // groovy.time.TimeCategory.getMinutes(lowPowerDelay as Integer)
		if(delayedTime <= new Date().getTime())
		{
			if(!atomicState.sentNotification)
			{
				if (debugOutput) log.debug "Sending Notification"
				atomicState.sentNotification = true
				send(lowPowerMessage) //TODO low power usage
				if(switchList) { switchList*.on() }
			}
		}
		else
		{
			if(!atomicState.isScheduled )
			{
				if (debugOutput) log.debug "Scheduling low power usage check."
				atomicState.isScheduled = true;
				runOnce( new Date(delayedTime), delayedPowerCheck)
			}
		}
	}
	if(latestPower > maxPowerThreshold)
	{
		if (debugOutput) log.debug "high power usage!"
		send(highPowerMessage)  
	}
}
 

 
 
 

 



private send(msg) {
	if (!msg) return // no message 
	if (textNotification) { textNotification*.deviceNotification(msg) }
	if (debugOutput) { log.debug "send: $msg" }
	if (speechOut) 
	{ 
		if(speechEchoAnnouncement)
		{
			speechOut*.playAnnouncementAll(msg)
		}
		else
		{
			speechOut*.speak(msg)
		}
	}
	if (player){ player*.playText(msg) }
}


def installed() {
	// Initialize the states only when first installed...
	atomicState.sentNotification = false
	atomicState.isScheduled = false
	atomicState.isLowPower = false
	atomicState.lastPowerUsage = new Date().getTime()

	if (switchList) switchList*.off() 
	
	initialize()
	app.clearSetting("debugOutput")	// app.updateSetting() only updates, won't create.
	app.clearSetting("descTextEnable")
	if (descTextEnable) log.info "Installed with settings: ${settings}"
}


def updated() {
	unsubscribe()
	unschedule()
	initialize()
	if (descTextEnable) log.info "Updated with settings: ${settings}"
}


def initialize() {
	if (atomicState.isPaused) {
		updateMyLabel()
		return
	}
	 
	subscribe(pwrMeter, "power", powerHandler)
	if (debugOutput) log.debug "Cycle: ${atomicState.cycleOn} thresholds: ${startThreshold} ${endThreshold} ${delayEndPwr}/${delayEndAcc}"
 
	updateMyLabel()
	
//	app.clearSetting("debugOutput")	// app.updateSetting() only updates, won't create.
//	app.clearSetting("descTextEnable") // un-comment these, click Done then replace the // comment
}

 
 


def setDebug(dbg, inf) {
	app.updateSetting("debugOutput",[value:dbg, type:"bool"])
	app.updateSetting("descTextEnable",[value:inf, type:"bool"])
	if (descTextEnable) log.info "debugOutput: $debugOutput, descTextEnable: $descTextEnable"
}


def display()
{
 
	section {
		paragraph "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
		paragraph "<div style='color:#1A77C9;text-align:center;font-weight:small;font-size:9px'>Developed by: Jonathan Porter, Kevin Tierney, ChrisUthe, C Steele, Barry Burke<br/>Version Status: $state.Status<br>Current Version: ${version()} -  ${thisCopyright}</div>"
    }
}
String getPausedText()
{
	return '<span style="color:Crimson"> (paused)</span>'
}
void updateMyLabel() {
	String myLabel =  appTitle
	if (atomicState.isPaused) {
		myLabel = myLabel +  getPausedText()
	} 
	if (app.label != myLabel) app.updateLabel(myLabel)
}
 
 

def getThisCopyright(){"&copy; 2019 Jonathan Porter "}