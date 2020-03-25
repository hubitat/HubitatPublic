/*
	Generic Z-Wave CentralScene Dimmer

	Copyright 2016 -> 2020 Hubitat Inc.  All Rights Reserved
	2020-02-04 2.1.9 maxwell
		-refactor
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
        0x20: 1     //basic
        ,0x26: 1    //switchMultiLevel
        ,0x5B: 1    //centralScene
]
@Field static Map switchVerbs = [0:"was turned",1:"is"]
@Field static Map levelVerbs = [0:"was set to",1:"is"]
@Field static Map switchValues = [0:"off",1:"on"]

metadata {
    definition (name: "Generic Z-Wave CentralScene Dimmer",namespace: "hubitat", author: "Mike Maxwell") {
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

        fingerprint deviceId: "3034", inClusters: "0x5E,0x86,0x72,0x5A,0x85,0x59,0x73,0x26,0x27,0x70,0x2C,0x2B,0x5B,0x7A", outClusters: "0x5B", mfr: "0315", prod: "4447", deviceJoinName: "ZWP WD-100 Dimmer"
        fingerprint deviceId: "3034", inClusters: "0x5E,0x86,0x72,0x5A,0x85,0x59,0x55,0x73,0x26,0x70,0x2C,0x2B,0x5B,0x7A,0x9F,0x6C", outClusters: "0x5B", mfr: "0315", prod: "4447", deviceJoinName: "ZLINK ZL-WD-100"
        fingerprint deviceId: "0334", inClusters: "0x26,0x27,0x2B,0x2C,0x72,0x86,0x91,0x77,0x73", mfr: "001D", prod: "1B03", deviceJoinName: "Leviton DXMX1"
        fingerprint deviceId: "0209", inClusters: "0x26,0x27,0x2B,0x2C,0x85,0x72,0x86,0x91,0x77,0x73", outClusters: "0x82", mfr: "001D", prod: "0401", deviceJoinName: "Leviton VRI06-1LZ"
        fingerprint deviceId: "0209", inClusters: "0x25,0x27,0x2B,0x2C,0x85,0x72,0x86,0x91,0x77,0x73", outClusters: "0x82", mfr: "001D", prod: "0301", deviceJoinName: "Leviton VRI10-1LZ"
        fingerprint deviceId: "0209", inClusters: "0x26,0x27,0x2B,0x2C,0x85,0x72,0x86,0x91,0x77,0x73", outClusters: "0x82", mfr: "001D", prod: "0501", deviceJoinName: "Leviton ???"
        fingerprint deviceId: "0334", inClusters: "0x26,0x27,0x2B,0x2C,0x85,0x72,0x86,0x91,0x77,0x73", outClusters: "0x82", mfr: "001D", prod: "0602", deviceJoinName: "Leviton ???"
        fingerprint deviceId: "0001", inClusters: "0x5E,0x85,0x59,0x86,0x72,0x70,0x5A,0x73,0x26,0x20,0x27,0x2C,0x2B,0x7A", outClusters: "0x82", mfr: "001D", prod: "3501", deviceJoinName: "Leviton DZPD3-2BW"
        fingerprint deviceId: "3034", inClusters: "0x5E,0x55,0x9F", outClusters: "0x5B", mfr: "000C", prod: "4447", deviceJoinName: "Homeseer HS-WD100+"
        fingerprint deviceId: "3034", inClusters: "0x5E,0x86,0x72,0x5A,0x85,0x59,0x55,0x73,0x26,0x70,0x2C,0x2B,0x5B,0x7A,0x9F,0x6C", outClusters: "0x5B", mfr: "000C", prod: "4447", deviceJoinName: "Homeseer HS-WD100+"
    }

    preferences {
        input name: "param4", type: "enum", title: "Paddle function", options:[[0:"Normal"],[1:"Reverse"]], defaultValue: 0
        input name: "flashRate", type: "enum", title: "Flash rate", options:[[750:"750ms"],[1000:"1s"],[2000:"2s"],[5000:"5s"]], defaultValue: 750
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

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
    if (getDataValue("zwaveSecurePairingComplete") != "true") {
        return cmd
    }
    Short S2 = getDataValue("S2")?.toInteger()
    String encap = ""
    String keyUsed = "S0"
    if (S2 == null) { //S0 existing device
        encap = "988100"
    } else if ((S2 & 0x04) == 0x04) { //S2_ACCESS_CONTROL
        keyUsed = "S2_ACCESS_CONTROL"
        encap = "9F0304"
    } else if ((S2 & 0x02) == 0x02) { //S2_AUTHENTICATED
        keyUsed = "S2_AUTHENTICATED"
        encap = "9F0302"
    } else if ((S2 & 0x01) == 0x01) { //S2_UNAUTHENTICATED
        keyUsed = "S2_UNAUTHENTICATED"
        encap = "9F0301"
    } else if ((S2 & 0x80) == 0x80) { //S0 on C7
        encap = "988100"
    }
    if (logEnable) log.trace "keyUsed:${keyUsed}"
    return "${encap}${cmd}"
}

String secure(hubitat.zwave.Command cmd){
    return secure(cmd.format())
}

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd){
    hubitat.zwave.Command encapCmd = cmd.encapsulatedCommand(commandClassVersions)
    if (encapCmd) {
        zwaveEvent(encapCmd)
    }
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

//returns on physical
void zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd){
    if (logEnable) log.debug "SwitchMultilevelReport value: ${cmd.value}"
    dimmerEvents(cmd.value,"physical")
}

//returns on digital
void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd){
    if (logEnable) log.info "BasicReport value: ${cmd.value}"
    dimmerEvents(cmd.value,"digital")
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
                runInMillis(200,delayHold,[data:button])
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
    if (ramp > 255) ramp = 255
    else if (ramp == 0) ramp = 1
    Integer delay = (ramp * 1000) + 200
    if (level > 99) level = 99

    return [
            secure(zwave.configurationV1.configurationSet(scaledConfigurationValue:  ramp, parameterNumber: 8, size: 2))
            ,"delay 200"
            ,secure(zwave.basicV1.basicSet(value: level))
            ,"delay ${delay}"
            ,secure(zwave.basicV1.basicGet())
    ]
}


List<String> on(){
    state.bin = -11
    state.flashing = false
    return delayBetween([
            secure(zwave.basicV1.basicSet(value: 0xFF))
            ,secure(zwave.basicV1.basicGet())
    ] ,200)
}

List<String> off(){
    state.bin = -10
    state.flashing = false
    return delayBetween([
            secure(zwave.basicV1.basicSet(value: 0x00))
            ,secure(zwave.basicV1.basicGet())
    ] ,200)
}

String flash(){
    if (txtEnable) log.info "${device.displayName} was set to flash with a rate of ${flashRate ?: 750} milliseconds"
    state.flashing = true
    return flashOn()
}

String flashOn(){
    if (!state.flashing) return
    runInMillis((flashRate ?: 750).toInteger(), flashOff)
    return secure(zwave.basicV1.basicSet(value: 0xFF))
}

String flashOff(){
    if (!state.flashing) return
    runInMillis((flashRate ?: 750).toInteger(), flashOn)
    return secure(zwave.basicV1.basicSet(value: 0x00))
}

String refresh(){
    if (logEnable) log.debug "refresh"
    state.bin = -2
    return secure(zwave.basicV1.basicGet())
}

void installed(){
    log.warn "installed..."
    sendEvent(name: "level", value: 20)
}

void configure(){
    log.warn "configure..."
    runIn(1800,logsOff)
    sendEvent(name: "numberOfButtons", value: 2)
	state."${1}" = 0
	state."${2}" = 0
    runIn(5, "refresh")
}

//capture preference changes
List<String> updated(){
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)

    List<String> cmds = []

    //paddle reverse function
    if (param4) {
        cmds.add(secure(zwave.configurationV1.configurationSet(scaledConfigurationValue: param4.toInteger(), parameterNumber: 4, size: 1)))
    }
    if (cmds) return cmds
}
