/*
	Generic Z-Wave CentralScene Dimmer

	Copyright 2016 -> 2020 Hubitat Inc.  All Rights Reserved
	2020-11-08  2.2.4 jvm - switched to using multilevel set v2 if a device supports it. More accurately calculates ramp delay parameters. Removed support for non-plus devices (they can use the default Hubitat drivers!)
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
        0x20: 2     //basic
        ,0x26: 3    //switchMultiLevel
        ,0x5B: 1    //centralScene
        ,0x70: 1    //configuration get
	,0x86: 2	// Version get
]
@Field static Map switchVerbs = [0:"was turned",1:"is"]
@Field static Map levelVerbs = [0:"was set to",1:"is"]
@Field static Map switchValues = [0:"off",1:"on"]

metadata {
    definition (name: "Modified Generic Z-Wave CentralScene Dimmer",namespace: "hubitat", author: "Mike Maxwell") {
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
        input name: "flashRate", type: "enum", title: "Flash rate", options:[[750:"750ms"],[1000:"1s"],[2000:"2s"],[5000:"5s"]], defaultValue: 750
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

void zwaveEvent(hubitat.zwave.commands.centralscenev1.CentralSceneNotification cmd){
    if (logEnable) log.debug "CentralSceneNotification: ${cmd}"

    Integer button = cmd.sceneNumber
    Integer key = cmd.keyAttributes
    String action
    switch (key){
        case 0: //pushed
            action = "pushed"
            break
        case 1:	//released, only after 2
            state."${button}" = 0
            action = "released"
            break
        case 2:	//holding
            if (state."${button}" == 0){
                state."${button}" = 1
                runInMillis(400,delayHold,[data:button])
            }
            break
        case 3:	//double tap, 4 is tripple tap
            action = "doubleTapped"
            break
    }

    if (action){
        sendButtonEvent(action, button, "physical")
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
	// Values greater than 127 need a special encoding, so simplify with use of 127
    if (ramp > 127) ramp = 127
    // else if (ramp == 0) ramp = 3
    Integer delay = (ramp * 1000) + 250
    if (level > 99) level = 99
    
	List<String> cmds = []
    
	if (state.commandVersions.get('38') > 1)
	{
        log.info "Sending value ${level} with delay ${ramp * 1000} mSec using switchMultilevel Version 2"
		
		cmds.add(secure(zwave.switchMultilevelV2.switchMultilevelSet(value: level, dimmingDuration: ramp)))
		// Switches supporting version 2 report using BasicReportv2 so no need to add delay as processing can use target value!
		// cmds.add("delay ${delay}")
		cmds.add(secure(zwave.basicV1.basicGet()))
		
		if(cmds) return cmds
	}
	else
	{
		log.info "Sending value ${level} with default ramp delay ${state.remoteRampTime} mSec using switchMultilevel Version 1"
		
		// cmds.add(secure(zwave.configurationV1.configurationSet(scaledConfigurationValue:  ramp, parameterNumber: 8, size: 2)))
		// cmds.add("delay 250")
		cmds.add(secure(zwave.switchMultilevelV1.switchMultilevelSet(value: level)))
		cmds.add("delay ${delay}")
		cmds.add(secure(zwave.basicV1.basicGet()))
		
		if(cmds) return cmds		
		
	}
}


List<String> on(){
    state.bin = -11
    state.flashing = false
    return delayBetween([
            secure(zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF))
            ,secure(zwave.basicV1.basicGet())
    ] , (state.remoteRampTime ?: 3000) + 250)
}

List<String> off(){
    state.bin = -10
    state.flashing = false
    return delayBetween([
            secure(zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00))
            ,secure(zwave.basicV1.basicGet())
    ] , (state.remoteRampTime ?: 3000) + 250)
}

String flash(){
    if (txtEnable) log.info "${device.displayName} was set to flash with a rate of ${flashRate ?: 750} milliseconds"
    state.flashing = true
    return flashOn()
}

String flashOn(){
    if (!state.flashing) return
    runInMillis((flashRate ?: 750).toInteger(), flashOff)
    return secure(zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF))
}

String flashOff(){
    if (!state.flashing) return
    runInMillis((flashRate ?: 750).toInteger(), flashOn)
    return secure(zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00))
}

String refresh(){
    if (logEnable) log.debug "refresh"
    state.bin = -2
    return secure(zwave.basicV1.basicGet())
}

void zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport  cmd) {
    log.info "Version Report Info ${cmd}"	
	state.versionReport = cmd
	}
	
List<String>   getDeviceInfo(){
	def cmds = [];
	
	List<Integer> ic = getDataValue("inClusters").split(",").collect{ hexStrToUnsignedInt(it) }
    ic.each {
		if (it) cmds.add(secure(zwave.versionV1.versionCommandClassGet(requestedCommandClass:it)))
    }

	// Software Version
	cmds.add(secure(zwave.versionV1.versionGet()))

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
	
List<String>   installed(){
    log.warn "installed..."
    sendEvent(name: "level", value: 20)
	

    List<String> cmds = []
		
	cmds = getDeviceInfo()
	
	
    if (cmds) return cmds

}


// Command class report - only really intersted in the multiLevelVersion report!
// Maybe expand to also include central scene report!
void zwaveEvent(hubitat.zwave.commands.versionv1.VersionCommandClassReport cmd) {
    log.info "CommandClassReport- class:${ "0x${intToHexStr(cmd.requestedCommandClass)}" }, version:${cmd.commandClassVersion}"	

    if (state.commandVersions == undefined) state.commandVersions = [:]
    
    state.commandVersions.put(cmd.requestedCommandClass, cmd.commandClassVersion)    
	
}	


List<String>  configure(){
    log.warn "configuring custom  Zwave Central Scene Dimmer Driver ..."
    runIn(1800,logsOff)
	
	// Clean up some state values from old versions of this update. 
        state.remove("basicVersion")
        state.remove("switchMultilevelVersion")
		
    sendEvent(name: "numberOfButtons", value: 2)
    state."${1}" = 0
    state."${2}" = 0
    runIn(5, "refresh")
    
    List<String> cmds = []
		cmds = getDeviceInfo()
    if (cmds) return cmds
}

//capture preference changes
List<String> updated(){
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)

    List<String> cmds = []
	
	cmds = getDeviceInfo()
	
    //paddle reverse function
    if (param4) {
        cmds.add(secure(zwave.configurationV1.configurationSet(scaledConfigurationValue: param4.toInteger(), parameterNumber: 4, size: 1)))
    }

    if (cmds) return cmds
}
