/*
	Generic Z-Wave CentralScene Dimmer

	Copyright 2016, 2017, 2018 Hubitat Inc.  All Rights Reserved
	2018-07-15 maxwell
	    -add all button commands
   2018-06-04 maxwell
        -updates to support changeLevel
   2018-03-26 maxwell
        -add standard level events algorithm
   2018-03-24 maxwell
        -initial pub

    zwp switch multilevel v1 (no duration)

*/

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

    }
    preferences {
        input name: "param4", type: "enum", title: "Paddle function", options:[[0:"Normal"],[1:"Reverse"]], defaultValue: 0
        input name: "flashRate", type: "enum", title: "Flash rate", options:[[750:"750ms"],[1000:"1s"],[2000:"2s"],[5000:"5s"]], defaultValue: 750
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true

    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def parse(String description) {
    if (logEnable) log.debug "parse description: ${description}"
    def cmd = zwave.parse(description,[ 0x26: 1])
    if (cmd) {zwaveEvent(cmd)}
    return
}

def startLevelChange(direction){
    def upDown = direction == "down" ? 1 : 0
    return zwave.switchMultilevelV1.switchMultilevelStartLevelChange(upDown: upDown, ignoreStartLevel: 1, startLevel: 0).format()
}

def stopLevelChange(){
    return [
        zwave.switchMultilevelV1.switchMultilevelStopLevelChange().format(),"delay 200",
        zwave.basicV1.basicGet().format()
    ]
}

//returns on physical
def zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd) {
    if (logEnable) log.debug "SwitchMultilevelReport value: ${cmd.value}"
    dimmerEvents(cmd.value,"physical")
}

//returns on digital
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    if (logEnable) log.info "BasicReport value: ${cmd.value}"
    dimmerEvents(cmd.value,"digital")
}

def dimmerEvents(rawValue,type) {
    if (logEnable) log.debug "dimmerEvents value: ${rawValue}, type: ${type}"

    def levelValue = rawValue.toInteger()
    //if (levelValue == 99) levelValue = 100

    def switchVerbs = [0:"was turned",1:"is"]
    def levelVerbs = [0:"was set to",1:"is"]
    def switchValues = [0:"off",1:"on"]
    def crntLevel = device.currentValue("level")?.toInteger()
    def crntSwitch = device.currentValue("switch") == "on" ? 1 : 0

    def switchText
    def levelText
    def switchValue
    //log.debug "bin: ${state.bin}, levelValue:${levelValue}, crntLevel: ${crntLevel}, crntSwitch: ${crntSwitch}, type: ${type}"
    switch(state.bin){
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
        default:
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

def zwaveEvent(hubitat.zwave.commands.centralscenev1.CentralSceneNotification cmd){
    if (logEnable) log.debug "CentralSceneNotification: ${cmd}"

    def button = cmd.sceneNumber
    def key = cmd.keyAttributes
    def action
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
        def descriptionText = "${device.displayName} button ${button} was ${action}"
        if (txtEnable) log.info "${descriptionText}"
        sendEvent(name: "${action}", value: "${button}", descriptionText: descriptionText, isStateChange: true, type: "physical")
    }
    return
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    if (logEnable) log.debug "skip: ${cmd}"
}

def delayHold(button){
    def descriptionText = "${device.displayName} button ${button} was held"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "held", value: "${button}", descriptionText: descriptionText, isStateChange: true, type: "physical")
}

def push(button){
    def descriptionText = "${device.displayName} button ${button} was pushed"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "pushed", value: "${button}", descriptionText: descriptionText, isStateChange: true, type: "digital")
}

def hold(button){
    def descriptionText = "${device.displayName} button ${button} was held"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "held", value: "${button}", descriptionText: descriptionText, isStateChange: true, type: "digital")
}

def release(button){
    def descriptionText = "${device.displayName} button ${button} was released"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "released", value: "${button}", descriptionText: descriptionText, isStateChange: true, type: "digital")
}

def doubleTap(button){
    def descriptionText = "${device.displayName} button ${button} was doubleTapped"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "doubleTapped", value: "${button}", descriptionText: descriptionText, isStateChange: true, type: "digital")
}

def setLevel(level){
    setLevel(level,1)
}

def setLevel(level,ramp){
    state.flashing = false
    state.bin = level
    if (ramp > 255) ramp = 255
    else if (ramp == 0) ramp = 1
    def delay = (ramp * 1000) + 200
    if (level > 99) level = 99

    return [
            zwave.configurationV1.configurationSet(scaledConfigurationValue:  ramp, parameterNumber: 8, size: 2).format(),"delay 200", //for zwp only
            zwave.basicV1.basicSet(value: level).format(), "delay ${delay}",
            zwave.basicV1.basicGet().format()
    ]
}


def on() {
    state.bin = -11
    state.flashing = false
    return delayBetween([zwave.basicV1.basicSet(value: 0xFF).format(),zwave.basicV1.basicGet().format()] ,200)
}

def off() {
    state.bin = -10
    state.flashing = false
    return delayBetween([zwave.basicV1.basicSet(value: 0x00).format(),zwave.basicV1.basicGet().format()] ,200)
}

def flash() {
    def descriptionText = "${device.getDisplayName()} was set to flash with a rate of ${flashRate} milliseconds"
    if (txtEnable) log.info "${descriptionText}"
    state.flashing = true
    flashOn()
}

def flashOn() {
    if(!state.flashing) return
    runInMillis(flashRate.toInteger(), flashOff)
    return [zwave.basicV1.basicSet(value: 0xFF).format()]
}

def flashOff() {
    if(!state.flashing) return
    runInMillis(flashRate.toInteger(), flashOn)
    return [zwave.basicV1.basicSet(value: 0x00).format()]
}

def refresh() {
    if (logEnable) log.debug "refresh"
    state.bin = -2
    return [zwave.basicV1.basicGet().format()]
}

def installed(){
    log.warn "installed..."
    sendEvent(name: "numberOfButtons", value: 2)
    for (i = 1; i <= 2; i++){
        state."${i}" = 0
    }
    sendEvent(name: "level", value: 20)
}

def configure() {
    log.warn "configure..."
    runIn(1800,logsOff)
    refresh()
}

//capture preference changes
def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)

    def cmds = []

    //paddle reverse function
    if (param4){
        cmds +=  zwave.configurationV1.configurationSet(scaledConfigurationValue: param4.toInteger(), parameterNumber: 4, size: 1).format()
    }
    if (cmds) return delayBetween(cmds, 500)
}
