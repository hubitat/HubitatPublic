
/*
	Third Reality Matter Multi-Function Night Light

	Copyright 2023 Hubitat Inc.  All Rights Reserved

	2023-11-02 2.3.7 maxwell
		-initial pub

*/

import groovy.transform.Field

//transitionTime options
@Field static Map ttOpts = [
        defaultValue: "1"
        ,defaultText: "1s"
        ,options:["0":"ASAP","1":"1s","2":"2s","5":"5s"]
]

@Field static Map colorRGBName = [
        4:"Red"
        ,13:"Orange"
        ,21:"Yellow"
        ,29:"Chartreuse"
        ,38:"Green"
        ,46:"Spring"
        ,54:"Cyan"
        ,63:"Azure"
        ,71:"Blue"
        ,79:"Violet"
        ,88:"Magenta"
        ,96:"Rose"
        ,101:"Red"
]

metadata {
    definition (name: "Third Reality Matter Multi-Function Night Light", namespace: "hubitat", author: "Mike Maxwell") {
        capability "Actuator"
        capability "Switch"
        capability "SwitchLevel"
        capability "Configuration"
        capability "Illuminance Measurement"
        capability "Color Control"
        capability "Light"
        capability "Motion Sensor"
        capability "Initialize"

        fingerprint endpointId:"01", inClusters:"0003,0004,0006,0008,001D,0300,0406", outClusters:"", model:"Smart Color Night Light", manufacturer:"ThirdReality", controllerType:"MAT"
        fingerprint endpointId:"01", inClusters:"0003,0004,0006,0008,001D,0300", outClusters:"", model:"Smart Color Night Light", manufacturer:"ThirdReality", controllerType:"MAT"

    }
    preferences {
        input(name:"transitionTime", type:"enum", title:"Level transition time (default:${ttOpts.defaultText})", options:ttOpts.options, defaultValue:ttOpts.defaultValue)
        input(name:"rgbTransitionTime", type:"enum", title:"RGB transition time (default:${ttOpts.defaultText})", options:ttOpts.options, defaultValue:ttOpts.defaultValue)
        input(name:"logEnable", type:"bool", title:"Enable debug logging", defaultValue:false)
        input(name:"txtEnable", type:"bool", title:"Enable descriptionText logging", defaultValue:true)
    }
}

//parsers
void parse(String description) {
    Map descMap = matter.parseDescriptionAsMap(description)
    if (logEnable) log.debug "descMap:${descMap}"
    switch (descMap.cluster) {
        case "0006" :
            if (descMap.attrId == "0000") { //switch
                sendSwitchEvent(descMap.value)
            }
            break
        case "0008" :
            if (descMap.attrId == "0000") { //current level
                sendLevelEvent(descMap.value)
            }
            break
        case "0000" :
            if (descMap.attrId == "4000") { //software build
                updateDataValue("softwareBuild",descMap.value ?: "unknown")
            }
            break
        case "0300" :
            if (descMap.attrId == "0000") { //hue
                sendHueEvent(descMap.value)
            } else if (descMap.attrId == "0001") { //saturation
                sendSaturationEvent(descMap.value)
            } //else log.trace "skipped color, attribute:${it.attrId}, value:${it.value}"
            break
        case "0400":
            if (descMap.attrId == "0000") {
                sendIlluminanceEvent(hexStrToUnsignedInt(descMap.value))
            }
            break
        case "0406" :
            if (descMap.attrId == "0000") {
                sendMotionEvent((descMap.value == "00") ? "inactive" : "active")
            }
            break
        default :
            if (logEnable) {
                log.debug "skipped:${descMap}"
            }
    }
}

//events
private void sendSwitchEvent(String rawValue) {
    String value = rawValue == "01" ? "on" : "off"
    if (device.currentValue("switch") == value) return
    String descriptionText = "${device.displayName} was turned ${value}"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"switch", value:value, descriptionText:descriptionText)
}

private void sendLevelEvent(String rawValue) {
    Integer value = Math.round(hexStrToUnsignedInt(rawValue) / 2.55)
    if (value == 0 || value == device.currentValue("level")) return
    String descriptionText = "${device.displayName} level was set to ${value}%"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"level", value:value, descriptionText:descriptionText, unit: "%")
}

private void sendHueEvent(String rawValue, Boolean presetColor = false) {
    Integer value = hex254ToInt100(rawValue)
    sendRGBNameEvent(value)
    String descriptionText = "${device.displayName} hue was set to ${value}%"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"hue", value:value, descriptionText:descriptionText, unit: "%")
}

private void sendSaturationEvent(String rawValue, Boolean presetColor = false) {
    Integer value = hex254ToInt100(rawValue)
    sendRGBNameEvent(null,value)
    String descriptionText = "${device.displayName} saturation was set to ${value}%"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"saturation", value:value, descriptionText:descriptionText, unit: "%")
}

private void sendRGBNameEvent(hue, sat = null){
    String genericName
    if (device.currentValue("saturation") == 0) {
        genericName = "White"
    } else if (hue == null) {
        return
    } else {
        genericName = colorRGBName.find{k , v -> hue < k}.value
    }
    if (genericName == device.currentValue("colorName")) return
    String descriptionText = "${device.displayName} color is ${genericName}"
    if (txtEnable) log.info descriptionText
    sendEvent(name: "colorName", value: genericName ,descriptionText: descriptionText)
}

void sendMotionEvent(value) {
    if (device.currentValue("motion") == value) return
    String descriptionText = "${device.displayName} is ${value}"
    if (txtEnable) log.info descriptionText
    sendEvent(name: "motion",value: value,descriptionText: descriptionText)
}

void sendIlluminanceEvent(rawValue) {
    Integer value = getLuxValue(rawValue)
    Integer pv = device.currentValue("illuminance") ?: 0
    if (pv == value) return
    String descriptionText = "${device.displayName} illuminance is ${value} Lux"
    if (txtEnable) log.info descriptionText
    sendEvent(name: "illuminance",value: value,descriptionText: descriptionText,unit: "Lux")
}

//capability commands
void on() {
    if (logEnable) log.debug "on()"
    sendToDevice(matter.on())
}

void off() {
    if (logEnable) log.debug "off()"
    sendToDevice(matter.off())
}

void setLevel(Object value) {
    if (logEnable) "setLevel(${value})"
    setLevel(value,transitionTime ?: 1)
}

void setLevel(Object value, Object rate) {
    if (logEnable) log.debug "setLevel(${value}, ${rate})"
    Integer level = value.toInteger()
    if (level == 0 && device.currentValue("switch") == "off") return
    sendToDevice(matter.setLevel(level, rate.toInteger()))
}

void setHue(Object value) {
    if (logEnable) log.debug "setHue(${value})"
    List<String> cmds = []
    Integer transitionTime = ( rgbTransitionTime ?: 1).toInteger()
    if (device.currentValue("switch") == "on"){
        cmds.add(matter.setHue(value.toInteger(), transitionTime))
    } else {
        cmds.add(matter.on())
        cmds.add(matter.setHue(value.toInteger(), transitionTime))
    }
    sendToDevice(cmds)
}

void setSaturation(Object value) {
    if (logEnable) log.debug "setSaturation(${value})"
    List<String> cmds = []
    Integer transitionTime = ( rgbTransitionTime ?: 1).toInteger()
    if (device.currentValue("switch") == "on"){
        cmds.add(matter.setSaturation(value.toInteger(), transitionTime))
    } else {
        cmds.add(matter.on())
        cmds.add(matter.setSaturation(value.toInteger(), transitionTime))
    }
    sendToDevice(cmds)
}

void setHueSat(Object hue, Object sat) {
    if (logEnable) log.debug "setHueSat(${hue}, ${sat})"
    List<String> cmds = []
    Integer transitionTime = ( rgbTransitionTime ?: 1).toInteger()
    if (device.currentValue("switch") == "on"){
        cmds.add(matter.setHue(hue.toInteger(), transitionTime))
        cmds.add(matter.setSaturation(sat.toInteger(), transitionTime))
    } else {
        cmds.add(matter.on())
        cmds.add(matter.setHue(hue.toInteger(), transitionTime))
        cmds.add(matter.setSaturation(sat.toInteger(), transitionTime))
    }
    sendToDevice(cmds)
}

void setColor(Map colorMap) {
    if (logEnable) log.debug "setColor(${colorMap})"
    if (colorMap.level) {
        setLevel(colorMap.level)
    }
    if (colorMap.hue != null && colorMap.saturation != null) {
        setHueSat(colorMap.hue, colorMap.saturation)
    } else if (colorMap.hue != null) {
        setHue(colorMap.hue)
    } else if (colorMap.saturation != null) {
        setSaturation(colorMap.saturation)
    }
}

void configure() {
    log.warn "configure..."
    sendToDevice(subscribeCmd())
}

//lifecycle commands
void updated(){
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

void initialize() {
    sendToDevice(subscribeCmd())
}

void refresh() {
    if (logEnable) log.debug "refresh()"
    sendToDevice(refreshCmd())
}

String refreshCmd() {
    List<Map<String, String>> attributePaths = []
    attributePaths.add(matter.attributePath(device.endpointId, 0x0006, 0x0000))
    attributePaths.add(matter.attributePath(device.endpointId, 0x0008, 0x0000))
    attributePaths.add(matter.attributePath(device.endpointId, 0x0300, 0x0000))
    attributePaths.add(matter.attributePath(device.endpointId, 0x0300, 0x0001))
    attributePaths.add(matter.attributePath(device.endpointId, 0x0300, 0x0007))
    attributePaths.add(matter.attributePath(device.endpointId, 0x0300, 0x0008))

    attributePaths.add(matter.attributePath(0x02, 0x0400, 0x0000)) //illuminance
    attributePaths.add(matter.attributePath(0x03, 0x0406, 0x0000)) //occupancy

    String cmd = matter.readAttributes(attributePaths)
    return cmd
}

String subscribeCmd() {
    List<Map<String, String>> attributePaths = []
    attributePaths.add(matter.attributePath(0x01, 0x0006, 0x00))
    attributePaths.add(matter.attributePath(0x01, 0x0008, 0x00))
    attributePaths.add(matter.attributePath(0x01, 0x0300, 0x00))
    attributePaths.add(matter.attributePath(0x01, 0x0300, 0x01))
    attributePaths.add(matter.attributePath(0x01, 0x0300, 0x07))
    attributePaths.add(matter.attributePath(0x01, 0x0300, 0x08))


    attributePaths.add(matter.attributePath(0x02, 0x0400, 0x0000)) //illuminance
    attributePaths.add(matter.attributePath(0x03, 0x0406, 0x0000)) //occupancy
    //standard 0 reporting interval is way too busy for bulbs
    String cmd = matter.subscribe(5,0xFFFF,attributePaths)
    return cmd
}

void logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

Integer hex254ToInt100(String value) {
    return Math.round(hexStrToUnsignedInt(value) / 2.54)
}

String int100ToHex254(value) {
    return intToHexStr(Math.round(value * 2.54))
}

Integer getLuxValue(rawValue) {
    return Math.max((Math.pow(10,(rawValue/10000)) - 1).toInteger(),1)
}

void sendToDevice(List<String> cmds, Integer delay = 300) {
    sendHubCommand(new hubitat.device.HubMultiAction(commands(cmds, delay), hubitat.device.Protocol.MATTER))
}

void sendToDevice(String cmd, Integer delay = 300) {
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

List<String> commands(List<String> cmds, Integer delay = 300) {
    return delayBetween(cmds.collect { it }, delay)
}

