/*
 	Halo Smoke Alarm

   Copyright 2016, 2017, 2018, 2019 Hubitat Inc.  All Rights Reserved
   2019-02-12 2.0.6 maxwell
       -update enrollResponse with delay
   2018-09-28 maxwell
       -initial pub

*/

import hubitat.zigbee.clusters.iaszone.ZoneStatus

metadata {
    definition (name: "Halo Smoke Alarm", namespace: "hubitat", author: "Mike Maxwell") {
        capability "Configuration"
        capability "Switch"
        capability "Switch Level"
        capability "Color Control"
        capability "Relative Humidity Measurement"
        capability "Temperature Measurement"
        capability "Pressure Measurement"
        capability "Smoke Detector"
        capability "Carbon Monoxide Detector"
        capability "Refresh"
        capability "Sensor"
        capability "Actuator"

        fingerprint inClusters: "0000,0001,0003,0402,0403,0405,0500,0502", manufacturer: "HaloSmartLabs", model: "haloWX", deviceJoinName: "Halo Smoke Alarm"

    }
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def updated(){
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

def installed(){
    sendEvent(name:"smoke", value:"clear")
    sendEvent(name:"carbonMonoxide", value:"clear")
}

def parse(String description) {
    if (logEnable) log.debug "parse: ${description}"

    def result = []

    if (description?.startsWith("enroll request")) {
        List cmds = zigbee.enrollResponse(1200)
        result = cmds?.collect { new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE) }
    } else {
        if (description?.startsWith("zone status")) {
            result = parseIasMessage(description)
        } else {
            result = parseReportAttributeMessage(description)
        }
    }
    return result
}

private parseReportAttributeMessage(String description) {
    def descMap = zigbee.parseDescriptionAsMap(description)
    if (logEnable) log.debug "zigbee.parseDescriptionAsMap-read attr: ${descMap}"
    def result = []
    def cluster = descMap.cluster ?: descMap.clusterId
    switch(cluster) {
        case "0405":
            getHumidityResult(descMap.value)
            break
        case "0402":
            getTemperatureResult(descMap.value)
            break
        case "0403":
            getPressureResult(descMap.value)
            break
        case "FD00":
            getAlarmResult(descMap.data[0])
            break
    /*
    case "FD01":	//
        log.debug "FD01- cmd:${descMap.command}, data:${descMap.data}"
        break
    case "FD02":	//
        //data:[03] ??
        log.debug "FD02- cmd:${descMap.command}, data:${descMap.data}"
        break
    case "0500":	//
        // cmd:04, data:[00]
        log.debug "0500- cmd:${descMap.command}, data:${descMap.data}"
        break
    */
        case "0006":	//switch events
            getSwitchResult(descMap.value)
            break
        case "0008":	//level Events
            getLevelResult(descMap.value)
            break
        case "0300":	//color events
            getColorResult(descMap.value,descMap.attrInt)
            break
        default :
            if (logEnable) log.warn "parseReportAttributeMessage- skip descMap:${descMap}, description:${description}"
            break
    }

    return result
}

private parseIasMessage(String description) {
    ZoneStatus zs = zigbee.parseZoneStatus(description)

    if (zs.alarm1) getAlarmResult("AL1")
    if (zs.alarm2) getAlarmResult("AL2")
    if (zs.alarm1 == 0 && zs.alarm2 == 0) getAlarmResult("00")

}

private getAlarmResult(rawValue){
    if (rawValue == null) return
    def value
    def name
    def descriptionText = "${device.displayName} "
    switch (rawValue) {
        case "00": //cleared
            if (device.currentValue("smoke") == "detected") {
                descriptionText = "${descriptionText} smoke is clear"
                sendEvent(name:"smoke", value:"clear",descriptionText:descriptionText)
            } else if (device.currentValue("carbonMonoxide") == "detected") {
                descriptionText = "${descriptionText} carbon monoxide is clear"
                sendEvent(name:"carbonMonoxide", value:"clear",descriptionText:descriptionText)
            } else {
                descriptionText = "${descriptionText} smoke and carbon monoxide are clear"
                sendEvent(name:"smoke", value:"clear")
                sendEvent(name:"carbonMonoxide", value:"clear")
            }
            break
    /*
    case "04":	//Elevated Smoke Detected (pre)
        descriptionText = "${descriptionText} smoke was detected (pre alert)"
        sendEvent(name:"smoke", value:"detected",descriptionText:"${descriptionText} smoke was detected")
        descriptionText = "${descriptionText} smoke was detected (pre alert)"
        break
    case "07":	//Smoke Detected, send again, force state change
        sendEvent(name:"smoke", value:"detected",descriptionText:"${descriptionText} smoke was detected", isStateChange: true)
        descriptionText = "${descriptionText} smoke was detected"
        break
    */
        case "09":	//Silenced
            log.debug "getAlarmResult- Silenced"
            break
        case "AL1":
            descriptionText = "${descriptionText} smoke was detected"
            sendEvent(name:"smoke", value:"detected",descriptionText:descriptionText, isStateChange: true)
            break
        case "AL2":
            descriptionText = "${descriptionText} carbon monoxide was detected"
            sendEvent(name:"carbonMonoxide", value:"detected",descriptionText:descriptionText, isStateChange: true)
            break
        default :
            descriptionText = "${descriptionText} getAlarmResult- skipped:${value}"
            return
    }
    if (txtEnable) log.info "${descriptionText}"
}

private getHumidityResult(valueRaw){
    if (valueRaw == null) return
    def value = Integer.parseInt(valueRaw, 16) / 100
    def descriptionText = "${device.displayName} RH is ${value}%"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:"humidity", value:value, descriptionText:descriptionText, unit:"%")
}

private getPressureResult(hex){
    if (hex == null) return
    def valueRaw = hexStrToUnsignedInt(hex)
    def value = valueRaw / 10
    def descriptionText = "${device.displayName} pressure is ${value}kPa"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:"pressure", value:value, descriptionText:descriptionText, unit:"kPa")
}

private getTemperatureResult(valueRaw){
    if (valueRaw == null) return
    def tempC = hexStrToSignedInt(valueRaw) / 100
    def value = convertTemperatureIfNeeded(tempC.toFloat(),"c",2)
    def unit = "°${location.temperatureScale}"
    def descriptionText = "${device.displayName} temperature is ${value}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:"temperature",value:value, descriptionText:descriptionText, unit:unit)
}

private getSwitchResult(rawValue){
    def value = rawValue == "01" ? "on" : "off"
    def name = "switch"
    if (device.currentValue("${name}") == value){
        descriptionText = "${device.displayName} is ${value}"
    } else {
        descriptionText = "${device.displayName} was turned ${value}"
    }
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:name,value:value,descriptionText:descriptionText)
}

private getLevelResult(rawValue){
    def unit = "%"
    def value = Math.round(Integer.parseInt(rawValue,16) / 2.55)
    def name = "level"
    if (device.currentValue("${name}") == value){
        descriptionText = "${device.displayName} is ${value}${unit}"
    } else {
        descriptionText = "${device.displayName} was set to ${value}${unit}"
    }
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:name,value:value,descriptionText:descriptionText,unit:unit)
}

private getColorResult(rawValue,attrInt){
    def unit = "%"
    def value = Math.round(Integer.parseInt(rawValue,16) / 2.55)
    def name
    switch (attrInt){
        case 0: //hue
            name = "hue"
            if (device.currentValue("${name}")?.toInteger() == value){
                descriptionText = "${device.displayName} ${name} is ${value}${unit}"
            } else {
                descriptionText = "${device.displayName} ${name} was set to ${value}${unit}"
            }
            state.lastHue = rawValue
            break
        case 1: //sat
            name = "saturation"
            if (device.currentValue("${name}")?.toInteger() == value){
                descriptionText = "${device.displayName} ${name} is ${value}${unit}"
            } else {
                descriptionText = "${device.displayName} ${name} was set to ${value}${unit}"
            }
            state.lastSaturation = rawValue
            break
        default :
            return
    }
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:name,value:value,descriptionText:descriptionText,unit:unit)
}

def configure() {
    log.warn "configure..."
    runIn(1800,logsOff)

    def cmds = [
            //bindings
            "zdo bind 0x${device.deviceNetworkId} 1 1 0x0402 {${device.zigbeeId}} {}", "delay 200",	//temp
            "zdo bind 0x${device.deviceNetworkId} 1 1 0x0403 {${device.zigbeeId}} {}", "delay 200",	//pressure
            "zdo bind 0x${device.deviceNetworkId} 1 1 0x0405 {${device.zigbeeId}} {}", "delay 200",	//hum
            "zdo bind 0x${device.deviceNetworkId} 2 1 0x0006 {${device.zigbeeId}} {}", "delay 200",
            "zdo bind 0x${device.deviceNetworkId} 2 1 0x0008 {${device.zigbeeId}} {}", "delay 200",
            "zdo bind 0x${device.deviceNetworkId} 2 1 0x0300 {${device.zigbeeId}} {}", "delay 200",
            //mfr specific
            "zdo bind 0x${device.deviceNetworkId} 4 1 0xFD00 {${device.zigbeeId}} {1201}", "delay 200",  //appears to be custom alarm
            "zdo bind 0x${device.deviceNetworkId} 4 1 0xFD01 {${device.zigbeeId}} {1201}", "delay 200", //hush???
            "zdo bind 0x${device.deviceNetworkId} 4 1 0xFD02 {${device.zigbeeId}} {1201}", "delay 200",
            //reporting
            "he cr 0x${device.deviceNetworkId} 1 0x0402 0x0000 0x29 1 43200 {50}","delay 200",	//temp
            "he cr 0x${device.deviceNetworkId} 1 0x0405 0x0000 0x21 1 43200 {50}","delay 200",	//hum
            "he cr 0x${device.deviceNetworkId} 1 0x0403 0x0000 0x29 1 43200 {1}","delay 200",	//pressure
            //mfr specific reporting
            "he cr 0x${device.deviceNetworkId} 0x04 0xFD00 0x0000 0x30 5 120 {} {1201}","delay 200",	//alarm
            "he cr 0x${device.deviceNetworkId} 0x04 0xFD00 0x0001 0x30 5 120 {} {1201}","delay 200",	//alarm
            //need to verify ep, st attrib id's 1, and 0
            "he cr 0x${device.deviceNetworkId} 0x01 0xFD01 0x0000 0x30 5 120 {} {1201}","delay 200",	//hush???
            "he cr 0x${device.deviceNetworkId} 0x01 0xFD01 0x0001 0x0A 5 120 {} {1201}","delay 200",	//hush???
            "he cr 0x${device.deviceNetworkId} 0x01 0xFD02 0x0000 0x29 5 120 {} {1201}","delay 200",	//no idea...

    ] + zigbee.enrollResponse(1200) + refresh()
    return cmds
}

def on() {
    def cmd = [
            "he cmd 0x${device.deviceNetworkId} 0x02 0x0006 1 {}","delay 200",
            "he rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}"
    ]
    return cmd
}

def off() {
    def cmd = [
            "he cmd 0x${device.deviceNetworkId} 0x02 0x0006 0 {}","delay 200",
            "he rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}"
    ]
    return cmd
}

def setLevel(value) {
    setLevel(value,(transitionTime?.toBigDecimal() ?: 1000) / 1000)
}

def setLevel(value,rate) {
    rate = rate.toBigDecimal()
    def scaledRate = (rate * 10).toInteger()
    def cmd = []
    def isOn = device.currentValue("switch") == "on"
    value = (value.toInteger() * 2.55).toInteger()
    if (isOn){
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 2 0x0008 4 {0x${intTo8bitUnsignedHex(value)} 0x${intTo16bitUnsignedHex(scaledRate)}}",
                "delay ${(rate * 1000) + 400}",
                "he rattr 0x${device.deviceNetworkId} 2 0x0008 0 {}"
        ]
    } else {
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 2 0x0008 4 {0x${intTo8bitUnsignedHex(value)} 0x0100}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 2 0x0008 0 {}"
        ]
    }
    return cmd
}


def setColor(value){
    if (value.hue == null || value.saturation == null) return

    def rate = transitionTime?.toInteger() ?: 1000
    def isOn = device.currentValue("switch") == "on"

    def hexSat = zigbee.convertToHexString(Math.round(value.saturation.toInteger() * 254 / 100).toInteger(),2)
    def level
    if (value.level) level = (value.level.toInteger() * 2.55).toInteger()
    def cmd = []
    def hexHue

    hexHue = zigbee.convertToHexString(Math.round(value.hue * 254 / 100).toInteger(),2)

    if (isOn){
        if (value.level){
            cmd = [
                    "he cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} ${intTo16bitUnsignedHex(rate / 100)}}","delay 200",
                    "he cmd 0x${device.deviceNetworkId} 2 0x0008 4 {0x${intTo8bitUnsignedHex(level)} 0x${intTo16bitUnsignedHex(rate / 100)}}",
                    "delay ${rate + 400}",
                    "he rattr 0x${device.deviceNetworkId} 2 0x0300 0x0000 {}", "delay 200",
                    "he rattr 0x${device.deviceNetworkId} 2 0x0300 0x0001 {}", "delay 200",
                    "he rattr 0x${device.deviceNetworkId} 2 0x0008 0 {}"
            ]
        } else {
            cmd = [
                    "he cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} ${intTo16bitUnsignedHex(rate / 100)}}",
                    "delay ${rate + 400}",
                    "he rattr 0x${device.deviceNetworkId} 2 0x0300 0x0000 {}", "delay 200",
                    "he rattr 0x${device.deviceNetworkId} 2 0x0300 0x0001 {}"
            ]
        }
    } else if (level){
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "he cmd 0x${device.deviceNetworkId} 2 0x0008 4 {0x${intTo8bitUnsignedHex(level)} 0x0100}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 2 0x0008 0 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 2 0x0300 0x0000 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 2 0x0300 0x0001 {}"
        ]
    } else {
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "he cmd 0x${device.deviceNetworkId} 2 0x0006 1 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 2 0x0300 0x0000 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 2 0x0300 0x0001 {}"
        ]
    }
    state.lastSaturation = hexSat
    state.lastHue = hexHue
    return cmd
}

def setHue(value) {
    def hexHue
    def rate = 1000
    def isOn = device.currentValue("switch") == "on"
    hexHue = zigbee.convertToHexString(Math.round(value * 254 / 100).toInteger(),2)
    def hexSat = state.lastSaturation
    def cmd = []
    if (isOn){
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} ${intTo16bitUnsignedHex(rate / 100)}}",
                "delay ${rate + 400}",
                "he rattr 0x${device.deviceNetworkId} 2 0x0300 0x0000 {}"
        ]
    } else {
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "he cmd 0x${device.deviceNetworkId} 2 0x0006 1 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 2 0x0300 0x0000 {}"
        ]
    }
    state.lastHue = hexHue
    return cmd
}

def setSaturation(value) {
    def rate = 1000
    def cmd = []
    def isOn = device.currentValue("switch") == "on"
    def hexSat = zigbee.convertToHexString(Math.round(value * 254 / 100).toInteger(),2)
    def hexHue = state.lastHue
    if (isOn){
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} ${intTo16bitUnsignedHex(rate / 100)}}",
                "delay ${rate + 400}",
                "he rattr 0x${device.deviceNetworkId} 2 0x0300 0x0001 {}"
        ]
    } else {
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 2 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "he cmd 0x${device.deviceNetworkId} 2 0x0006 1 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 2 0x0300 0x0001 {}"
        ]
    }
    state.lastSaturation = hexSat
    return cmd
}

def refresh() {
    if (logEnable) log.debug "refresh"
    return  [
            "he rattr 0x${device.deviceNetworkId} 1 0x0402 0x0000 {}","delay 200",  //temp OK
            "he rattr 0x${device.deviceNetworkId} 1 0x0403 0x0000 {}","delay 200",  // pressure
            "he rattr 0x${device.deviceNetworkId} 1 0x0405 0x0000 {}" ,"delay 200", //hum OK
            "he rattr 0x${device.deviceNetworkId} 2 0x0006 0 {}","delay 200",  //light state
            "he rattr 0x${device.deviceNetworkId} 2 0x0008 0 {}","delay 200",  //light level
            "he rattr 0x${device.deviceNetworkId} 2 0x0300 0x0000 {}","delay 200", //hue
            "he rattr 0x${device.deviceNetworkId} 2 0x0300 0x0001 {}" //sat
    ]

}


def intTo16bitUnsignedHex(value) {
    def hexStr = zigbee.convertToHexString(value.toInteger(),4)
    return new String(hexStr.substring(2, 4) + hexStr.substring(0, 2))
}

def intTo8bitUnsignedHex(value) {
    return zigbee.convertToHexString(value.toInteger(), 2)
}

