/*
    Ring Keypad Gen 2

    Copyright 2020 -> 2021 Hubitat Inc.  All Rights Reserved
    2021-07-26 2.2.8 bcopeland
        -lockCodes capability
    2021-05-10 2.2.8 maxwell
        -remove custom commands
    2021-02-23 2.2.6 bcopeland
        -add command debug log
    2021-01-07 2.2.5 bcopeland
        -battery report update
    2020-12-29 2.2.5 bcopeland
        - Rework for misunderstanding of HSM workings
        - update security bits
    2020-07-14 2.2.2 bcopeland
        - bug fix in updated()
	2020-07-01 2.2.2 bcopeland
		-initial pub

*/

import groovy.transform.Field
import groovy.json.JsonOutput

metadata {

    definition (name: "Ring Alarm Keypad G2", namespace: "hubitat", author: "Bryan Copeland") {
        capability "Actuator"
        capability "Sensor"
        capability "Configuration"
        capability "SecurityKeypad"
        capability "Battery"
        capability "Alarm"
        capability "PowerSource"
        capability "LockCodes"

        command "entry"
        command "setArmNightDelay", ["number"]
        command "setArmHomeDelay", ["number"]
        command "setPartialFunction"

        attribute "armingIn", "NUMBER"
        attribute "lastCodeName", "STRING"

        fingerprint mfr:"0346", prod:"0101", deviceId:"0301", inClusters:"0x5E,0x98,0x9F,0x6C,0x55", deviceJoinName: "Ring Alarm Keypad G2"
    }
    preferences {
        configParams.each { input it.value.input }
        input name: "optEncrypt", type: "bool", title: "Enable lockCode encryption", defaultValue: false, description: ""
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

@Field static Map configParams = [
        4: [input: [name: "configParam4", type: "enum", title: "Announcement Volume", description:"", defaultValue:7, options:[0:"0",1:"1",2:"2",3:"3",4:"4",5:"5",6:"6",7:"7",8:"8",9:"9",10:"10"]],parameterSize:1],
        5: [input: [name: "configParam5", type: "enum", title: "Keytone Volume", description:"", defaultValue:6, options:[0:"0",1:"1",2:"2",3:"3",4:"4",5:"5",6:"6",7:"7",8:"8",9:"9",10:"10"]],parameterSize:1],
        6: [input: [name: "configParam6", type: "enum", title: "Siren Volume", description:"", defaultValue:10, options:[0:"0",1:"1",2:"2",3:"3",4:"4",5:"5",6:"6",7:"7",8:"8",9:"9",10:"10"]],parameterSize:1]
]
@Field static Map armingStates = [
        0x00: [securityKeypadState: "armed night", hsmCmd: "armNight"],
        0x02: [securityKeypadState: "disarmed", hsmCmd: "disarm"],
        0x0A: [securityKeypadState: "armed home", hsmCmd: "armHome"],
        0x0B: [securityKeypadState: "armed away", hsmCmd: "armAway"]
]
@Field static Map CMD_CLASS_VERS=[0x86:2, 0x70:1, 0x20:1, 0x86:3]

void logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value:"false", type:"bool"])
}

void updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    log.warn "encryption is: ${optEncrypt == true}"
    unschedule()
    if (logEnable) runIn(1800,logsOff)
    sendToDevice(runConfigs())
    updateEncryption()
}

void installed() {
    initializeVars()
}

void uninstalled() {

}

void initializeVars() {
    // first run only
    sendEvent(name:"codeLength", value: 4)
    sendEvent(name:"maxCodes", value: 100)
    sendEvent(name:"lockCodes", value: "")
    sendEvent(name:"securityKeypad", value:"disarmed")
    state.keypadConfig=[entryDelay:5, exitDelay: 5, armNightDelay:5, armHomeDelay: 5, codeLength: 4, partialFunction: "armHome"]
    state.keypadStatus=2
    state.initialized=true
}

void configure() {
    if (logEnable) log.debug "configure()"
    if (!state.initialized) initializeVars()
    if (!state.keypadConfig) initializeVars()
    keypadUpdateStatus(state.keypadStatus)
    runIn(5,pollDeviceData)
}

void pollDeviceData() {
    List<String> cmds = []
    cmds.add(zwave.versionV3.versionGet().format())
    cmds.add(zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType: 1).format())
    cmds.add(zwave.batteryV1.batteryGet().format())
    cmds.add(zwave.notificationV8.notificationGet(notificationType: 8, event: 0).format())
    cmds.add(zwave.notificationV8.notificationGet(notificationType: 7, event: 0).format())
    cmds.addAll(processAssociations())
    sendToDevice(cmds)
}

void keypadUpdateStatus(Integer status,String type="digital", String code) {
    sendToDevice(zwave.indicatorV3.indicatorSet(indicatorCount:1, value: 0, indicatorValues:[[indicatorId:status, propertyId:2, value:0xFF]]).format())
    state.keypadStatus = status
    if (state.code != "") { type = "physical" }
    eventProcess(name: "securityKeypad", value: armingStates[status].securityKeypadState, type: type, data: state.code)
    state.code = ""
    state.type = "digital"
}

void armNight(delay=0) {
    if (logEnable) log.debug "armNight(${delay})"
    if (delay > 0 ) {
        exitDelay(delay)
        runIn(delay, armHomeEnd)
    }
}

void armAway(delay=0) {
    if (logEnable) log.debug "armAway(${delay})"
    if (delay > 0 ) {
        exitDelay(delay)
        runIn(delay, armAwayEnd)
    } else {
        armAwayEnd()
    }
}

void armAwayEnd() {
    if (!state.code) { state.code = "" }
    if (!state.type) { state.type = "physical" }
    keypadUpdateStatus(0x0B, state.type, state.code)
    //sendLocationEvent(name: "hsmSetArm", value: "armAway")
}

void disarm(delay=0) {
    if (logEnable) log.debug "disarm(${delay})"
    if (delay > 0 ) {
        exitDelay(delay)
        runIn(delay, disarmEnd)
    } else {
        disarmEnd()
    }
}

void disarmEnd() {
    if (!state.code) { state.code = "" }
    if (!state.type) { state.type = "physical" }
    keypadUpdateStatus(0x02, state.type, state.code)
    //sendLocationEvent(name: "hsmSetArm", value: "disarm")
}

void armHome(delay=0) {
    if (logEnable) log.debug "armHome(${delay})"
    if (delay > 0) {
        exitDelay(delay)
        runIn(delay+1, armHomeEnd)
    } else {
        armHomeEnd()
    }
}

private armHomeEnd() {
    if (!state.code) { state.code = "" }
    if (!state.type) { state.type = "physical" }
    keypadUpdateStatus(0x0A, state.type, state.code)
}

void exitDelay(delay){
    if (logEnable) log.debug "exitDelay(${delay})"
    log.info "Exit delay ${delay}"
    if (delay) {
        sendToDevice(zwave.indicatorV3.indicatorSet(indicatorCount:1, value: 0, indicatorValues:[[indicatorId:0x12, propertyId:7, value:delay.toInteger()]]).format())
        //runIn(delay.toInteger(), "checkHsm")
    }
}

private void checkHsm() {
    switch (location.hsmStatus) {
        case "armedHome":
            keypadUpdateStatus(0x0A)
            break
        case "armedNight":
            keypadUpdateStatus(0x0A)
            break
        case "armedAway":
            keypadUpdateStatus(0x0B)
            break
        case "disarmed":
            keypadUpdateStatus(0x02)
            break
        default:
            //runIn(5,"checkHsm")
            break
    }
}

void setEntryDelay(delay){
    if (logEnable) log.debug "setEntryDelay(${delay})"
    state.keypadConfig.entryDelay = delay != null ? delay.toInteger() : 0
}

void setExitDelay(Map delays){
    if (logEnable) log.debug "setExitDelay(${delays})"
    state.keypadConfig.exitDelay = (delays?.awayDelay ?: 0).toInteger()
    state.keypadConfig.armNightDelay = (delays?.nightDelay ?: 0).toInteger()
    state.keypadConfig.armHomeDelay = (delays?.homeDelay ?: 0).toInteger()
}

void setExitDelay(delay){
    if (logEnable) log.debug "setExitDelay(${delay})"
    state.keypadConfig.exitDelay = delay != null ? delay.toInteger() : 0
}

void setArmNightDelay(delay){
    if (logEnable) log.debug "setArmNightDelay(${delay})"
    state.keypadConfig.armNightDelay = delay != null ? delay.toInteger() : 0
}

void setArmHomeDelay(delay){
    if (logEnable) log.debug "setArmHomeDelay(${delay})"
    state.keypadConfig.armHomeDelay = delay != null ? delay.toInteger() : 0

}
void setCodeLength(pincodelength) {
    if (logEnable) log.debug "setCodeLength(${pincodelength})"
    eventProcess(name:"codeLength", value: pincodelength, descriptionText: "${device.displayName} codeLength set to ${pincodelength}")
    state.keypadConfig.codeLength = pincodelength
    // set zwave entry code key buffer
    // 6F06XX10
    sendToDevice("6F06" + hubitat.helper.HexUtils.integerToHexString(pincodelength.toInteger()+1,1).padLeft(2,'0') + "0F")
}

void setPartialFunction(mode = null) {
    if (logEnable) log.debug "setPartialFucntion(${mode})"
    if ( !(mode in ["armHome","armNight"]) ) {
        if (txtEnable) log.warn "custom command used by HSM"
    } else if (mode in ["armHome","armNight"]) {
        state.keypadConfig.partialFunction = mode == "armHome" ? "armHome" : "armNight"
    }
}

// alarm capability commands

void off() {
    if (logEnable) log.debug "off()"
    eventProcess(name:"alarm", value:"off")
    sendToDevice(zwave.indicatorV3.indicatorSet(indicatorCount:1, value: 0, indicatorValues:[[indicatorId:state.keypadStatus, propertyId:2, value:0xFF]]).format())
}

void both() {
    if (logEnable) log.debug "both()"
    siren()
}

void siren() {
    if (logEnable) log.debug "siren()"
    eventProcess(name:"alarm", value:"siren")
    sendToDevice(zwave.indicatorV3.indicatorSet(indicatorCount:1, value: 0, indicatorValues:[[indicatorId:0x0C, propertyId:2, value:0xFF]]).format())
}

void strobe() {
    if (logEnable) log.debug "strobe()"
    eventProcess(name:"alarm", value:"strobe")
    List<String> cmds=[]
    cmds.add(zwave.indicatorV3.indicatorSet(indicatorCount:1, value: 0, indicatorValues:[[indicatorId:0x0C, propertyId:2, value:0xFF]]).format())
    cmds.add(zwave.indicatorV3.indicatorSet(indicatorCount:1, value: 0, indicatorValues:[[indicatorId:0x0C, propertyId:2, value:0x00]]).format())
    sendToDevice(cmds)
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    // this is redundant/ambiguous and I don't care what happens here
}

void parseEntryControl(Short command, List<Short> commandBytes) {
    //log.debug "parse: ${command}, ${commandBytes}"
    if (command == 0x01) {
        Map ecn = [:]
        ecn.sequenceNumber = commandBytes[0]
        ecn.dataType = commandBytes[1]
        ecn.eventType = commandBytes[2]
        ecn.eventDataLength = commandBytes[3]
        String code=""
        if (ecn.eventDataLength>0) {
            for (int i in 4..(ecn.eventDataLength+3)) {
                if (logEnable) log.debug "character ${i}, value ${commandBytes[i]}"
                code += (char) commandBytes[i]
            }
        }
        if (logEnable) log.debug "Entry control: ${ecn} keycache: ${code}"
        switch (ecn.eventType) {
            case 5:
                if (validatePin(code)) {
                    state.type="physical"
                    if (!state.keypadConfig.exitDelay) { state.keypadConfig.exitDelay = 0 }
                    sendEvent(name:"armingIn", value: state.keypadConfig.exitDelay, data:[armMode: armingStates[0x0B].securityKeypadState, armCmd: armingStates[0x0B].hsmCmd], isStateChange:true)
                    //armAway()
                } else {
                    sendToDevice(zwave.indicatorV3.indicatorSet(indicatorCount:1, value: 0, indicatorValues:[[indicatorId:0x09, propertyId:2, value:0xFF]]).format())
                }
                break
            case 6:
                if (validatePin(code)) {
                    state.type="physical"
                    if(!state.keypadConfig.partialFunction) state.keypadConfig.partialFunction="armHome"
                    if (state.keypadConfig.partialFunction == "armHome") {
                        if (!state.keypadConfig.armHomeDelay) { state.keypadConfig.armHomeDelay = 0 }
                        sendEvent(name:"armingIn", value: state.keypadConfig.armHomeDelay, data:[armMode: armingStates[0x0A].securityKeypadState, armCmd: armingStates[0x0A].hsmCmd], isStateChange:true)
                    } else {
                        if (!state.keypadConfig.armNightDelay) { state.keypadConfig.armNightDelay = 0 }
                        sendEvent(name:"armingIn", value: state.keypadConfig.armNightDelay, data:[armMode: armingStates[0x00].securityKeypadState, armCmd: armingStates[0x00].hsmCmd], isStateChange:true)
                    }
                    //armHome()
                } else {
                    sendToDevice(zwave.indicatorV3.indicatorSet(indicatorCount:1, value: 0, indicatorValues:[[indicatorId:0x09, propertyId:2, value:0xFF]]).format())
                }
                break
            case 3:
                if (validatePin(code)) {
                    state.type="physical"
                    sendEvent(name:"armingIn", value: 0, data:[armMode: armingStates[0x02].securityKeypadState, armCmd: armingStates[0x02].hsmCmd], isStateChange:true)
                    //disarm()
                } else {
                    sendToDevice(zwave.indicatorV3.indicatorSet(indicatorCount:1, value: 0, indicatorValues:[[indicatorId:0x09, propertyId:2, value:0xFF]]).format())
                }
                break
        }
    }
}

void zwaveEvent(hubitat.zwave.commands.notificationv8.NotificationReport cmd) {
    Map evt = [:]
    if (cmd.notificationType == 8) {
        // power management
        switch (cmd.event) {
            case 1:
                // Power has been applied
                if (txtEnable) log.info "${device.displayName} Power has been applied"
                break
            case 2:
                // AC mains disconnected
                evt.name = "powerSource"
                evt.value = "battery"
                evt.descriptionText = "${device.displayName} AC mains disconnected"
                eventProcess(evt)
                break
            case 3:
                // AC mains re-connected
                evt.name = "powerSource"
                evt.value = "mains"
                evt.descriptionText = "${device.displayName} AC mains re-connected"
                eventProcess(evt)
                break
            case 12:
                // battery is charging
                if (txtEnable) log.info "${device.displayName} Battery is charging"
                break
        }
    }
}

void entry(){
    int intDelay = state.keypadConfig.entryDelay ? state.keypadConfig.entryDelay.toInteger() : 0
    if (intDelay) entry(intDelay)
}

void entry(entranceDelay){
    if (logEnable) log.debug "entry(${entranceDelay})"
    if (entranceDelay) {
        sendToDevice(zwave.indicatorV3.indicatorSet(indicatorCount:1, value: 0, indicatorValues:[[indicatorId:0x11, propertyId:7, value:entranceDelay.toInteger()]]).format())
    }
}

void getCodes(){
    if (logEnable) log.debug "getCodes()"
    updateEncryption()
}

private updateEncryption(){
    String lockCodes = device.currentValue("lockCodes") //encrypted or decrypted
    if (lockCodes){
        if (optEncrypt && lockCodes[0] == "{") {	//resend encrypted
            sendEvent(name:"lockCodes",value: encrypt(lockCodes), isStateChange:true)
        } else if (!optEncrypt && lockCodes[0] != "{") {	//resend decrypted
            sendEvent(name:"lockCodes", value: decrypt(lockCodes), isStateChange:true)
        } else {
            sendEvent(name:"lockCodes", value: lockCodes, isStateChange:true)
        }
    }
}

private Boolean validatePin(String pincode) {
    boolean retVal = false
    Map lockcodes = [:]
    if (optEncrypt) {
        lockcodes = parseJson(decrypt(device.currentValue("lockCodes")))
    } else {
        lockcodes = parseJson(device.currentValue("lockCodes"))
    }
    //log.debug "Lock codes: ${lockcodes}"
    lockcodes.each {
        if(it.value["code"] == pincode) {
            log.debug "found code: ${pincode} user: ${it.value['name']}"
            sendEvent(name:"lastCodeName", value: "${it.value['name']}", isStateChange:true)
            retVal=true
            String code = JsonOutput.toJson(["${it.key}":["name": "${it.value.name}", "code": "${it.value.code}", "isInitiator": true]])
            if (optEncrypt) {
                state.code=encrypt(code)
            } else {
                state.code=code
            }
        }
    }
    return retVal
}

void setCode(codeposition, pincode, name) {
    if (logEnable) log.debug "setCode(${codeposition}, ${pincode}, ${name})"
    boolean newCode = true
    Map lockcodes = [:]
    if (device.currentValue("lockCodes") != null) {
        if (optEncrypt) {
            lockcodes = parseJson(decrypt(device.currentValue("lockCodes")))
        } else {
            lockcodes = parseJson(device.currentValue("lockCodes"))
        }
    }
    if (lockcodes["${codeposition}"]) { newCode = false }
    lockcodes["${codeposition}"] = ["code": "${pincode}", "name": "${name}"]
    if (optEncrypt) {
        sendEvent(name: "lockCodes", value: encrypt(JsonOutput.toJson(lockcodes)))
    } else {
        sendEvent(name: "lockCodes", value: JsonOutput.toJson(lockcodes), isStateChange: true)
    }
    if (newCode) {
        sendEvent(name: "codeChanged", value:"added")
    } else {
        sendEvent(name: "codeChanged", value: "changed")
    }
    //log.debug "Lock codes: ${lockcodes}"
}

void deleteCode(codeposition) {
    if (logEnable) log.debug "deleteCode(${codeposition})"
    Map lockcodes=[:]
    if (device.currentValue("lockCodes") != null) {
        if (optEncrypt) {
            lockcodes = parseJson(decrypt(device.currentValue("lockCodes")))
        } else {
            lockcodes = parseJson(device.currentValue("lockCodes"))
        }
    }
    lockcodes["${codeposition}"] = [:]
    lockcodes.remove("${codeposition}")
    if (optEncrypt) {
        sendEvent(name: "lockCodes", value: encrypt(JsonOutput.toJson(lockcodes)))
    } else {
        sendEvent(name: "lockCodes", value: JsonOutput.toJson(lockcodes), isStateChange: true)
    }
    sendEvent(name: "codeChanged", value: "deleted")
    //log.debug "remove ${codeposition} Lock codes: ${lockcodes}"
}

void zwaveEvent(hubitat.zwave.commands.indicatorv3.IndicatorReport cmd) {
    // Don't need to handle reports
}

// standard config

List<String> runConfigs() {
    List<String> cmds = []
    configParams.each { param, data ->
        if (settings[data.input.name]) {
            cmds.addAll(configCmd(param, data.parameterSize, settings[data.input.name]))
        }
    }
    return cmds
}

List<String> pollConfigs() {
    List<String> cmds = []
    configParams.each { param, data ->
        if (settings[data.input.name]) {
            cmds.add(zwave.configurationV1.configurationGet(parameterNumber: param.toInteger()).format())
        }
    }
    return cmds
}

List<String> configCmd(parameterNumber, size, scaledConfigurationValue) {
    List<String> cmds = []
    cmds.add(zwave.configurationV1.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), scaledConfigurationValue: scaledConfigurationValue.toInteger()).format())
    cmds.add(zwave.configurationV1.configurationGet(parameterNumber: parameterNumber.toInteger()).format())
    return cmds
}

void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    if(configParams[cmd.parameterNumber.toInteger()]) {
        Map configParam = configParams[cmd.parameterNumber.toInteger()]
        int scaledValue
        cmd.configurationValue.reverse().eachWithIndex { v, index ->
            scaledValue = scaledValue | v << (8 * index)
        }
        device.updateSetting(configParam.input.name, [value: "${scaledValue}", type: configParam.input.type])
    }
}

// Battery v1

void zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
    Map evt = [name: "battery", unit: "%"]
    if (cmd.batteryLevel == 0xFF) {
        evt.descriptionText = "${device.displayName} has a low battery"
        evt.value = 1
    } else {
        evt.value = cmd.batteryLevel
        evt.descriptionText = "${device.displayName} battery is ${evt.value}${evt.unit}"
    }
    evt.isStateChange = true
    if (txtEnable && evt.descriptionText) log.info evt.descriptionText
    sendEvent(evt)
}

// MSP V2

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) {
    if (logEnable) log.debug "Device Specific Report - DeviceIdType: ${cmd.deviceIdType}, DeviceIdFormat: ${cmd.deviceIdDataFormat}, Data: ${cmd.deviceIdData}"
    if (cmd.deviceIdType == 1) {
        String serialNumber = ""
        if (cmd.deviceIdDataFormat == 1) {
            cmd.deviceIdData.each { serialNumber += hubitat.helper.HexUtils.integerToHexString(it & 0xff,1).padLeft(2, '0')}
        } else {
            cmd.deviceIdData.each { serialNumber += (char) it }
        }
        device.updateDataValue("serialNumber", serialNumber)
    }
}

// Version V2

void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
    Double firmware0Version = cmd.firmware0Version + (cmd.firmware0SubVersion / 100)
    Double protocolVersion = cmd.zWaveProtocolVersion + (cmd.zWaveProtocolSubVersion / 100)
    if (logEnable) log.debug "Version Report - FirmwareVersion: ${firmware0Version}, ProtocolVersion: ${protocolVersion}, HardwareVersion: ${cmd.hardwareVersion}"
    device.updateDataValue("firmwareVersion", "${firmware0Version}")
    device.updateDataValue("protocolVersion", "${protocolVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
    if (cmd.firmwareTargets > 0) {
        cmd.targetVersions.each { target ->
            Double targetVersion = target.version + (target.subVersion / 100)
            device.updateDataValue("firmware${target.target}Version", "${targetVersion}")
        }
    }
}

// Association V2

List<String> setDefaultAssociation() {
    List<String> cmds = []
    cmds.add(zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId).format())
    cmds.add(zwave.associationV2.associationGet(groupingIdentifier: 1).format())
    return cmds
}

List<String> processAssociations(){
    List<String> cmds = []
    cmds.addAll(setDefaultAssociation())
    return cmds
}

void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
    List<String> temp = []
    if (cmd.nodeId != []) {
        cmd.nodeId.each {
            temp.add(it.toString().format( '%02x', it.toInteger() ).toUpperCase())
        }
    }
    if (logEnable) log.debug "Association Report - Group: ${cmd.groupingIdentifier}, Nodes: $temp"
}

// event filter

void eventProcess(Map evt) {
    if (txtEnable && evt.descriptionText) log.info evt.descriptionText
    if (device.currentValue(evt.name).toString() != evt.value.toString()) {
        sendEvent(evt)
    }
}

// universal

void zwaveEvent(hubitat.zwave.Command cmd) {
    if (logEnable) log.debug "skip:${cmd}"
}

void zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    hubitat.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    }
}

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd) {
    if (logEnable) log.debug "Supervision Get - SessionID: ${cmd.sessionID}, CC: ${cmd.commandClassIdentifier}, Command: ${cmd.commandIdentifier}"
    if (cmd.commandClassIdentifier == 0x6F) {
        parseEntryControl(cmd.commandIdentifier, cmd.commandByte)
    } else {
        hubitat.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
        if (encapsulatedCommand) {
            zwaveEvent(encapsulatedCommand)
        }
    }
    // device quirk requires this to be unsecure reply
    sendToDevice(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0).format())
    //sendHubCommand(new hubitat.device.HubAction(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0).format(), hubitat.device.Protocol.ZWAVE))
}

void parse(String description) {
    if (logEnable) log.debug "parse:${description}"
    hubitat.zwave.Command cmd = zwave.parse(description, CMD_CLASS_VERS)
    if (cmd) {
        zwaveEvent(cmd)
    }
}

void sendToDevice(List<String> cmds, Long delay=300) {
    sendHubCommand(new hubitat.device.HubMultiAction(commands(cmds, delay), hubitat.device.Protocol.ZWAVE))
}

void sendToDevice(String cmd, Long delay=300) {
    sendHubCommand(new hubitat.device.HubAction(zwaveSecureEncap(cmd), hubitat.device.Protocol.ZWAVE))
}

List<String> commands(List<String> cmds, Long delay=300) {
    return delayBetween(cmds.collect{ zwaveSecureEncap(it) }, delay)
}
