/*
	Generic Z-Wave CentralScene Dimmer

	Copyright 2016 -> 2020 Hubitat Inc.  All Rights Reserved
	2020-11-12  2.2.4 jvm 
		- Queries device for parameters 7-10 and uses the results to calculate local and remote delays
		- Gather's command class versions at install / configure / update time to enable more advanced functions
		- switched to using multilevel set v2 if a device supports it - for improved dimming rate control
		- Removed code that sets remote ramping delay by changing device parameter during the level action 
			- if device doesn't support multilevel v2, then always use the timing set by the device parameters 
				- changing device ramping parameters while attempting to set level is unpredictable and caused errors!.
		- Removed support for non-plus devices (they can use the default Hubitat drivers!). 
		- Fixed Central Scenehold refresh. Use slowRefresh timing
	2020--07-31 2.2.3 maxwell
	    -switch to internal secure encap method
	2020-06-01 2.2.1 bcopeland
	    -basicSet to switchMultilevelSet conversion
	2020-03-25 2.2.0 maxwell
	    -C7/S2 updates
	    -refactor
	    -remove DZMX1 (move to generic dimmer)
	    -add supervision report response
	2019-11-14 2.1.7 maxwell
	    -add safe nav on flashrate
	    -add command class versions
	    -move some leviton dimmers over from generic
	2018-07-15 maxwell
	    -add all button commands
	2018-06-04 maxwell
	    -updates to support changeLevel
	2018-03-26 maxwell
	    -add standard level events algorithm
	2018-03-24 maxwell
	    -initial pub

*/
import groovy.transform.Field

@Field static Map commandClassVersions = [
        0x20: 2     //basic v2 preferred as it specifies target value!
        ,0x26: 3    //switchMultiLevel
        ,0x5B: 3    //centralScene
        ,0x70: 1    //configuration get
	,0x86: 2	// Version get
]
@Field static Map switchVerbs = [0:"was turned",1:"is"]
@Field static Map levelVerbs = [0:"was set to",1:"is"]
@Field static Map switchValues = [0:"off",1:"on"]

metadata {
    definition (name: "Generic Z-Wave Plus CentralScene Dimmer",namespace: "hubitat", author: "Based on code from Mike Maxwell") {
        capability "Actuator"
        capability "Switch"
        capability "Switch Level"
        capability "ChangeLevel"
        capability "Configuration"
        capability "PushableButton"
        capability "HoldableButton"
        capability "ReleasableButton"
        capability "DoubleTapableButton"

        command "flash"
        command "refresh"
        command "push", ["NUMBER"]
        command "hold", ["NUMBER"]
        command "release", ["NUMBER"]
        command "doubleTap", ["NUMBER"]
										
	command "setParameter",[[name:"parameterNumber",type:"NUMBER", description:"Parameter Number", constraints:["NUMBER"]],
				[name:"size",type:"NUMBER", description:"Parameter Size", constraints:["NUMBER"]],
				[name:"value",type:"NUMBER", description:"Parameter Value", constraints:["NUMBER"]]
				]
										

        fingerprint deviceId: "3034", inClusters: "0x5E,0x86,0x72,0x5A,0x85,0x59,0x73,0x26,0x27,0x70,0x2C,0x2B,0x5B,0x7A", outClusters: "0x5B", mfr: "0315", prod: "4447", deviceJoinName: "ZWP WD-100 Dimmer"
        fingerprint deviceId: "3034", inClusters: "0x26,0x2B,0x2C,0x55,0x59,0x5A,0x5B,0x5E,0x6C,0x70,0x72,0x73,0x7A,0x85,0x86,0x9F", outClusters: "0x5B", mfr: "0315", prod: "4447", deviceJoinName: "ZLINK ZL-WD-100"
	fingerprint deviceId: "3034", inClusters: "0x26,0x2B,0x2C,0x55,0x59,0x5A,0x5B,0x5E,0x6C,0x70,0x72,0x73,0x7A,0x85,0x86,0x9F", outClusters: "0x5B", mfr: "000C", prod: "4447", deviceJoinName: "Homeseer HS-WD100+"

    }

    preferences {
        input name: "param4", type: "enum", title: "Paddle function", options:[[0:"Normal"],[1:"Reverse"]], defaultValue: 0
        input name: "flashRate", type: "enum", title: "Flash rate", options:[[1:"1s"],[2:"2s"],[5:"5s"]], defaultValue: 1
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}


List<String> setParameter(parameterNumber = null, size = null, value = null){
    if (parameterNumber == null || size == null || value == null) {
		log.warn "incomplete parameter list supplied..."
		log.info "syntax: setParameter(parameterNumber,size,value)"
    } else {
		return delayBetween([
	    	secure(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: parameterNumber, size: size)),
	    	secure(zwave.configurationV1.configurationGet(parameterNumber: parameterNumber))
		],500)
    }
}

//Z-Wave responses
void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
	log.info "Firmware Version Report is: ${cmd}"
	state.fwVersion = cmd
}
//cmds

void logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void parse(String description){
    if (logEnable) log.debug "parse description: ${description}"
    hubitat.zwave.Command cmd = zwave.parse(description,commandClassVersions)
    if (cmd) {
        zwaveEvent(cmd)
    }
}

String secure(String cmd){
    return zwaveSecureEncap(cmd)
}

String secure(hubitat.zwave.Command cmd){
    return zwaveSecureEncap(cmd)
}

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd){
    hubitat.zwave.Command encapCmd = cmd.encapsulatedCommand(commandClassVersions)
    if (encapCmd) {
        zwaveEvent(encapCmd)
    }
    sendHubCommand(new hubitat.device.HubAction(secure(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0)), hubitat.device.Protocol.ZWAVE))
}

String startLevelChange(direction){
    Integer upDown = direction == "down" ? 1 : 0
    return secure(zwave.switchMultilevelV1.switchMultilevelStartLevelChange(upDown: upDown, ignoreStartLevel: 1, startLevel: 0))
}

List<String> stopLevelChange(){
    return [
            secure(zwave.switchMultilevelV1.switchMultilevelStopLevelChange())
            ,"delay 200"
            ,secure(zwave.basicV1.basicGet())
    ]
}

void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    
	if (state.parameters == undefined) state.parameters = [:]
    
	state.parameters.put(cmd.parameterNumber, cmd.scaledConfigurationValue)

	state.remoteRampTime = Math.round( (state.parameters.get('8') ?: 3)  * 1000 / (state.parameters.get('7') ?: 1))
	state.localRampTime = Math.round((state.parameters.get('10') ?: 3) * 1000 / (state.parameters.get('9') ?: 1))
}

//returns on physical v1
void zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd){
    if (logEnable) log.debug "SwitchMultilevelV1Report value: ${cmd}"
    dimmerEvents(cmd.value,"physical")
}

//returns on physical v2
void zwaveEvent(hubitat.zwave.commands.switchmultilevelv2.SwitchMultilevelReport cmd){
    if (logEnable) log.debug "SwitchMultilevelV2Report value: ${cmd}"
    dimmerEvents(cmd.value,"physical")
}

//returns on physical v3
void zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd){
    if (logEnable) log.debug "SwitchMultilevelV3Report value: ${cmd}"
    dimmerEvents(cmd.value,"physical")
}

//returns on digital
void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd){
    if (logEnable) log.info "BasicReport value: ${cmd}"
    dimmerEvents(cmd.value,"digital")
}

//returns on digital v2
void zwaveEvent(hubitat.zwave.commands.basicv2.BasicReport cmd){
    if (logEnable) log.info "BasicReport V2  is: ${cmd}"
    dimmerEvents(cmd.targetValue,"digital")
}

void forceReleaseHold(button){
	// This function operates as a backup in case a release report was lost on the network
	// It will force a release to be sent if there has been a hold event and then
	// a release has not occurred within the central scene hold button refresh period.
	// The central scene hold button refresh period is 200 mSec for old devices (state.slowRefresh == false), else it is 55 seconds.
	// Thus, adding in extra time for network delays, etc., this forces release after either 1 or 60 seconds 
    if (state."${button}" == 1)
	{
		// only need to force a release hold if the button state is 1 when the timer expires
		sendButtonEvent("released", button, "physical")
	}
	state."${button}" = 0
}

void zwaveEvent(hubitat.zwave.commands.centralscenev2.CentralSceneNotification cmd){
    if (logEnable) log.debug "CentralSceneNotification: ${cmd}"

    Integer button = cmd.sceneNumber
    Integer key = cmd.keyAttributes
    String action
    switch (key){
        case 0: //pushed
            sendButtonEvent("pushed", button, "physical")
			state."${button}" = 0
            break
        case 1:	//released, only after 2
            state."${button}" = 0
            sendButtonEvent("released", button, "physical")
            break
        case 2:	//holding
		    // The first time you get a hold, send the hold event
			// If the release has not occurred, assuming you are getting a refresh event and suppress sending another hold.
			// Suppress using the "runIn" to set a timer which 
            if (state."${button}" == 0){
			    sendButtonEvent("held", button, "physical")
                state."${button}" = 1
				// Assume slow refresh rate of 55 seconds.
                runIn( 60,forceReleaseHold,[data:button])		   
				// Alternatively, could check for slowRefresh timing, but easier not to.	
                // runIn( (state.slowRefresh ? 60 : 1 ),forceReleaseHold,[data:button])
            }
			else
			{
				if (logEnable) log.debug "Continuing hold of button ${button}"
                		runIn( 60,forceReleaseHold,[data:button])				
                		// runIn( (state.slowRefresh ? 60 : 1 ),forceReleaseHold,[data:button])
			}
            break
        case 3:	//double tap, 4 is tripple tap
			sendButtonEvent("doubleTapped", button, "physical")
			state."${button}" = 0
            break
    }
}

void zwaveEvent(hubitat.zwave.Command cmd){
    if (logEnable) log.debug "skip: ${cmd}"
}

void dimmerEvents(rawValue,type){
    if (logEnable) log.debug "dimmerEvents value: ${rawValue}, type: ${type}"
    Integer levelValue = rawValue.toInteger()
    Integer crntLevel = (device.currentValue("level") ?: 50).toInteger()
    Integer crntSwitch = (device.currentValue("switch") == "on") ? 1 : 0

    String switchText
    String levelText
    String switchValue

    switch(state.bin) {
        case -1:
            if (levelValue == 0){
                switchValue = switchValues[0]
                switchText = "${switchVerbs[crntSwitch ^ 1]} ${switchValue}"// --c1" //xor
            } else {
                switchValue = switchValues[1]
                switchText = "${switchVerbs[crntSwitch & 1]} ${switchValue}"// --c3"
                if (levelValue == crntLevel) levelText = "${levelVerbs[1]} ${crntLevel}%"// --c3a"
                else levelText = "${levelVerbs[0]} ${levelValue}%"// --c3b"
            }
            break
        case 0..100: //digital set level -basic report
            switchValue = switchValues[levelValue ? 1 : 0]
            switchText = "${switchVerbs[crntSwitch & 1]} ${switchValue}"// --c4"
            if (levelValue == 0) levelValue = 1
            levelText = "${levelVerbs[levelValue == crntLevel ? 1 : 0]} ${levelValue}%"// --c4"
            break
        case -11: //digital on -basic report
            switchValue = switchValues[1]
            switchText = "${switchVerbs[crntSwitch & 1]} ${switchValue}"// --c5"
            break
        case -10: //digital off -basic report
            switchValue = switchValues[0]
            switchText = "${switchVerbs[crntSwitch ^ 1]} ${switchValue}"// --c6"
            break
        case -2: //refresh digital -basic report
            if (levelValue == 0){
                switchValue = switchValues[0]
                switchText = "${switchVerbs[1]} ${switchValue}"// --c10"
                levelText = "${levelVerbs[1]} ${crntLevel}%"// --c10"
                levelValue = crntLevel
            } else {
                switchValue = switchValues[1]
                switchText = "${switchVerbs[1]} ${switchValue}"// --c11"
                levelText = "${levelVerbs[1]} ${levelValue}%"// --c11"
            }
            break
        default :
            log.debug "missing- bin: ${state.bin}, levelValue:${levelValue}, crntLevel: ${crntLevel}, crntSwitch: ${crntSwitch}, type: ${type}"
            break
    }

    if (switchText){
        switchText = "${device.displayName} ${switchText} [${type}]"
        if (txtEnable) log.info "${switchText}"
        sendEvent(name: "switch", value: switchValue, descriptionText: switchText, type:type)
    }
    if (levelText){
        levelText = "${device.displayName} ${levelText} [${type}]"
        if (txtEnable) log.info "${levelText}"
        sendEvent(name: "level", value: levelValue, descriptionText: levelText, type:type,unit:"%")
    }
    state.bin = -1
}

void delayHold(button){
    sendButtonEvent("held", button, "physical")
}

void push(button){
    sendButtonEvent("pushed", button, "digital")
}

void hold(button){
    sendButtonEvent("held", button, "digital")
}

void release(button){
    sendButtonEvent("released", button, "digital")
}

void doubleTap(button){
    sendButtonEvent("doubleTapped", button, "digital")
}

void sendButtonEvent(action, button, type){
    String descriptionText = "${device.displayName} button ${button} was ${action} [${type}]"
    if (txtEnable) log.info descriptionText
    sendEvent(name:action, value:button, descriptionText:descriptionText, isStateChange:true, type:type)
}

List<String> setLevel(level){
    return setLevel(level,1)
}

List<String> setLevel(level,ramp){
    state.flashing = false
    state.bin = level
	// Values greater than 127 signify minutes and users may not realize they are setting huge delays --  simplify with use of 127 as the maximum.
    if (ramp > 127) ramp = 127
    if (ramp < 0) ramp = 0
    if (level > 99) level = 99
	if (level < 0) level = 0
    
	List<String> cmds = []
    
	if (state.commandVersions.get('38') > 1)
	{
        if (logEnable) log.debug "Sending value ${level} with delay ${ramp * 1000} mSec using switchMultilevel Version 2"
		
		
		cmds.add(secure(zwave.switchMultilevelV2.switchMultilevelSet(value: level, dimmingDuration: ramp)))
		// Switches supporting version 2 will report using BasicReportv2 so you don't need
		// to wait for switch to complete transition before you get the return report as v2 report has the target value and 
		// you can use that target value to know what the new value will be when the transition completes.!
		cmds.add(secure(zwave.basicV1.basicGet()))
		
		if(cmds) return cmds
	}
	else
	{
		if (logEnable) log.debug "Sending value ${level} with default ramp delay ${state.remoteRampTime} mSec using switchMultilevel Version 1"
		
		// Versions 2.2.3 and lower used to reset the ramp rate. This can be a problematic operation. Better to ignore and use the ramp rate set by the user in the configuration parameters.
		// cmds.add(secure(zwave.configurationV1.configurationSet(scaledConfigurationValue:  ramp, parameterNumber: 8, size: 2)))
		// cmds.add("delay 250")
		cmds.add(secure(zwave.switchMultilevelV1.switchMultilevelSet(value: level)))
		cmds.add("delay ${(state.remoteRampTime ?: 3000) + 250}")
		cmds.add(secure(zwave.basicV1.basicGet()))
		
		if(cmds) return cmds		
		
	}
}


List<String> on(){
    state.bin = -11
    state.flashing = false
	
	def cmds = [];
	cmds.add(secure(zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF)))
	
	if (state.commandVersions.get('32') > 1)
	{
		// Don't need a delay if Basic Report v2 is available since you know the new value based on the reported
		// target value and you don't have to wait the entire ramp period!
		cmds.add(secure(zwave.basicV2.basicGet()))	
	}
	else
	{
		// else if only the older basic report is available, have to wait the full time.
		cmds.add("delay ${(state.remoteRampTime ?: 3000) + 250}")
		cmds.add(secure(zwave.basicV1.basicGet()))	
	}
    return cmds
}

List<String> off(){
    state.bin = -10
    state.flashing = false
	
	def cmds = [];
	cmds.add(secure(zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00)))
	
	if (state.commandVersions.get('32') > 1)
	{
		// Don't need a delay if Basic Report v2 is available since you know the new value based on the reported
		// target value and you don't have to wait the entire ramp period!
		cmds.add(secure(zwave.basicV2.basicGet()))	
	}
	else
	{
		// else if only the older basic report is available, have to wait the full time.
		cmds.add("delay ${(state.remoteRampTime ?: 3000) + 250}")
		cmds.add(secure(zwave.basicV1.basicGet()))	
	}
	return cmds
}

String flash(){
    if (txtEnable) log.info "${device.displayName} was set to flash with a rate of ${flashRate ?: 1} seconds"

	if (state.commandVersions.get('38') == 1)
	{
		log.warn "Your Zwave device may not properly display device flashing due to lack of support of updated protocol"
	}
    state.flashing = true
    return flashOn()
}

String flashOn(){
    if (!state.flashing) return
	
	def cmds = [];
	log.info "Flashing On"
	
	if (state.commandVersions.get('38') > 1)
	{
        if (logEnable) log.debug "Sending value ${level} with delay ${ramp * 1000} mSec using switchMultilevel Version 2"
		cmds.add(secure(zwave.switchMultilevelV2.switchMultilevelSet(value: 0xFF, dimmingDuration: ((flashRate ?: 1).toInteger()))))
	}
	else
	{
		cmds.add (secure(zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF)))
	}
	cmds.add("delay ${(flashRate).toInteger()}")

    runIn((flashRate ?: 1).toInteger(), flashOff)
	
    return cmds
}

String flashOff(){
    if (!state.flashing) return
    runIn((flashRate ?: 1).toInteger(), flashOn)
	
	def cmds = [];
	log.info "Flashing Off"
	
	if (state.commandVersions.get('38') > 1)
	{
        if (logEnable) log.debug "Sending value ${level} with delay ${ramp * 1000} mSec using switchMultilevel Version 2"
		cmds.add(secure(zwave.switchMultilevelV2.switchMultilevelSet(value: 0x00, dimmingDuration: ((flashRate ?: 1).toInteger()))))
	}
	else
	{
		cmds.add (secure(zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00)))
	}
	cmds.add("delay ${(flashRate).toInteger()}")
	
    return cmds
}

String refresh(){
    if (logEnable) log.debug "refresh"
    state.bin = -2
    return secure(zwave.basicV1.basicGet())
}

void zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport  cmd) {
    if(logEnable) log.debug "Version Report Info ${cmd}"	
	state.versionReport = cmd
	}
	
void zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneConfigurationReport  cmd) {
    if(logEnable) log.debug "Central Scene V3 Configuration Report Info ${cmd}"	
	state.slowRefresh = cmd.slowRefresh;
	}
	
List<String>   getDeviceInfo(){
	def cmds = [];
	
	List<Integer> ic = getDataValue("inClusters").split(",").collect{ hexStrToUnsignedInt(it) }
    ic.each {
		if (it) cmds.add(secure(zwave.versionV1.versionCommandClassGet(requestedCommandClass:it)))
    }

	// Software Version
	cmds.add(secure(zwave.versionV1.versionGet()))
	
	// Set central Scene Slow Refresh
	if (state.commandVersions.get('91') > 2)
	{
		if (logEnable) log.debug "Querying for Central Scene Configuration"
		cmds.add(secure(hubitat.zwave.commands.centralscenev3.CentralSceneConfigurationGet() ))
	}

	// Toggle Switch Orientation
	cmds.add(secure(zwave.configurationV1.configurationGet(parameterNumber: 4)))
	
	// Remote Ramp Rate Steps
	cmds.add(secure(zwave.configurationV1.configurationGet(parameterNumber: 7)))
	
	// Remote Ramp Rate Speed
	cmds.add(secure(zwave.configurationV1.configurationGet(parameterNumber: 8)))
	
	// local Ramp Rate Steps
	cmds.add(secure(zwave.configurationV1.configurationGet(parameterNumber: 9)))
	
	// local Ramp Rate Speed
	cmds.add(secure(zwave.configurationV1.configurationGet(parameterNumber: 10)))   
	
	return cmds
}



// Command class report - Primary interest in this driver are the multiLevelVersion and CentralScene reports!
// Maybe expand to also include central scene report!
void zwaveEvent(hubitat.zwave.commands.versionv1.VersionCommandClassReport cmd) {
    log.info "CommandClassReport- class:${ "0x${intToHexStr(cmd.requestedCommandClass)}" }, version:${cmd.commandClassVersion}"	

    if (state.commandVersions == undefined) state.commandVersions = [:]
    
    state.commandVersions.put(cmd.requestedCommandClass, cmd.commandClassVersion)    
	
}	
	
List<String>   installed(){

    log.warn "installed Generic Z-Wave Plus CentralScene Dimmer ..."
	state.slowRefresh = false
	
    sendEvent(name: "level", value: 20)

    List<String> cmds = []
		
	cmds = getDeviceInfo()
	
    if (cmds) return cmds

}

List<String>  configure(){
    log.warn "configuring Generic Z-Wave Plus CentralScene Dimmer ..."
    runIn(1800,logsOff)
	state.slowRefresh = false	
	
    sendEvent(name: "numberOfButtons", value: 2)
	
	// These state values are used to track whether you are in a Central Scene button holding state
    state."${1}" = 0
    state."${2}" = 0
    runIn(5, "refresh")
    
    List<String> cmds = []
		cmds = getDeviceInfo()
    if (cmds) return cmds
}

//capture preference changes
List<String> updated(){
    log.info "updated Generic Z-Wave Plus CentralScene Dimmer ..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
	state.slowRefresh = false	

    if (logEnable) runIn(1800,logsOff)

    List<String> cmds = []
	
	cmds = getDeviceInfo()
	
    //paddle reverse function
    if (param4) {
        cmds.add(secure(zwave.configurationV1.configurationSet(scaledConfigurationValue: param4.toInteger(), parameterNumber: 4, size: 1)))
    }

    if (cmds) return cmds
}
