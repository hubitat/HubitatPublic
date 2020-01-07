/*
 	Generic ZigBee RGBW Light

    Copyright 2016 -> 2020 Hubitat Inc.  All Rights Reserved
	2019-12-20 2.1.8 maxwell
		-add Nue HGZB-18A
		-add RGBGenie ZB-1025
    2019-11-14 2.1.7 maxwell
        -add GLEDOPTO Mini Zigbee RGB+CCT Controller
        -add Nue HGZB-07A
        -add Innr GU10 Colour bulb
	2019-09-02 2.1.5 maxwell
	    -add white as a color
        -change bindings to advertised endpoint
    2019-05-28 2.1.1 maxwell
        -fp update
    2019-01-05 2.0.5 maxwell
        -fp update
    2018-12-05 2.0.2 maxwell
        -fp update
    2018-10-12 ravenel
    	-add rate to setColor
    2018-06-04 maxwell
        -updates to support changeLevel
        -add capability color mode
	2018-04-09 maxwell
		-add transition and color staging options
    2018-03-24 maxwell
        -patch NPE on config log settings
*/

metadata {
    definition (name: "Generic ZigBee RGBW Light", namespace: "hubitat", author: "Mike Maxwell") {
        capability "Actuator"
        capability "Color Control"
        capability "Color Temperature"
        capability "Configuration"
        capability "Refresh"
        capability "Switch"
        capability "Switch Level"
        capability "ChangeLevel"
        capability "Light"
        capability "ColorMode"

        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B04,FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY Flex RGBW", deviceJoinName: "SYLVANIA Smart Flex RGBW"
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B04,FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "Flex RGBW", deviceJoinName: "OSRAM LIGHTIFY Flex RGBW"
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY A19 RGBW", deviceJoinName: "SYLVANIA Smart A19 RGBW"
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B04,FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY BR RGBW", deviceJoinName: "SYLVANIA Smart BR30 RGBW"
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B04,FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY RT RGBW", deviceJoinName: "SYLVANIA Smart RT5/6 RGBW"
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B04,FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY FLEX OUTDOOR RGBW", deviceJoinName: "SYLVANIA Smart Outdoor RGBW Flex"
        fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LCT001", deviceJoinName: "Philips Hue Lux Bulb"
        fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000,FC01", outClusters: "0019", manufacturer: "Philips", model: "LCT001", deviceJoinName: "Philips Hue Lux Bulb"
        fingerprint profileId: "C05E", inClusters: "1000,0000,0003,0004,0005,0006,0008,0300,FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "CLA60 RGBW OSRAM", deviceJoinName: "OSRAM Classic E27 Multicolor"
        fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LST002", deviceJoinName: "Philips Hue White and Color Ambiance LightStrip"
        fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,1000,0300", outClusters: "0019", manufacturer: "Philips", model: "LST002", deviceJoinName: "Philips Hue White and Color Ambiance LightStrip"
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B05,FC01", outClusters: "0019", manufacturer: "LEDVANCE", model: "BR30 RGBW", deviceJoinName: "SYLVANIA Smart + RGBW"
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B05,FC03", outClusters: "0019", manufacturer: "sengled", model: "E1G-G8E", deviceJoinName: "Sengled RGBW LED Strip"
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B05,FC01", outClusters: "0019", manufacturer: "LEDVANCE", model: "A19 RGBW", deviceJoinName: "SYLVANIA Smart + RGBW"
        fingerprint profileId:"C05E", endpointId:"0B", inClusters:"0000,0003,0004,0005,0006,0008,0300", model:"NUET56-DL27LX1.1", manufacturer:"3A Smart Home DE", deviceJoinName: "Nue HGZB-07A"
        fingerprint profileId:"C05E", endpointId:"0B", inClusters:"0000,0003,0004,0005,0006,0008,0300", model:"GL-MC-001", manufacturer:"GLEDOPTO", deviceJoinName: "GLEDOPTO Mini Zigbee RGB+CCT Controller"
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B05,1000,FC82", outClusters: "0019", manufacturer: "innr", model: "RS 230 C", deviceJoinName: "Innr GU10 Colour bulb"
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0004,0003,0006,0008,0005,0300,1000,0019", outClusters:"0019", model:"LXT56-LS27LX1.4", manufacturer:"3A Smart Home DE", deviceJoinName: "Nue HGZB-18A strip controller"
        fingerprint profileId:"C05E", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0008,0300,0B05,1000", outClusters:"0019,1000", model:"RGBgenie   ZB-1025", manufacturer:"RGBgenie", deviceJoinName: "RGBGenie ZB-1025 strip controller"
    }

    preferences {
        input name: "transitionTime", type: "enum", description: "", title: "Transition time", options: [[500:"500ms"],[1000:"1s"],[1500:"1.5s"],[2000:"2s"],[5000:"5s"]], defaultValue: 1000
        input name: "colorStaging", type: "bool", description: "", title: "Enable color pre-staging", defaultValue: false
        input name: "hiRezHue", type: "bool", title: "Enable Hue in degrees (0-360)", defaultValue: false
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
    log.warn "Hue in degrees is: ${hiRezHue == true}"
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

def parse(String description) {
    if (logEnable) log.debug "parse description: ${description}"
    if (description.startsWith("catchall")) return
    def descMap = zigbee.parseDescriptionAsMap(description)
    def descriptionText
    def rawValue = Integer.parseInt(descMap.value,16)
    def value
    def name
    def unit
    switch (descMap.clusterInt){
        case 6: //switch
            if (descMap.attrInt == 0){
                value = rawValue == 1 ? "on" : "off"
                name = "switch"
                if (device.currentValue("${name}") && value == device.currentValue("${name}")){
                    descriptionText = "${device.displayName} is ${value}"
                } else {
                    descriptionText = "${device.displayName} was turned ${value}"
                }
            } else {
                log.debug "0x0006:${descMap.attrId}:${rawValue}"
            }
            break
        case 8: //level
            if (descMap.attrInt == 0){
                unit = "%"
                value = Math.round(rawValue / 2.55)
                name = "level"
                if (device.currentValue("${name}") && value == device.currentValue("${name}").toInteger()){
                    descriptionText = "${device.displayName} is ${value}${unit}"
                } else {
                    descriptionText = "${device.displayName} was set to ${value}${unit}"
                }
            } else {
                log.debug "0x0008:${descMap.attrId}:${rawValue}"
            }
            break
        case 0x300: //color
            switch (descMap.attrInt){
                case 0: //hue
                    if (hiRezHue){
                        //hue is 0..360, value is 0..254 Hue = CurrentHue x 360 / 254
                        value = Math.round(rawValue * 360 / 254)
                        if (value == 361) value = 360
                        unit = "°"
                    } else {
                        value = Math.round(rawValue / 254 * 100)
                        unit = "%"
                    }
                    name = "hue"
                    if (device.currentValue("${name}") && value == device.currentValue("${name}").toInteger()){
                        descriptionText = "${device.displayName} ${name} is ${value}${unit}"
                    } else {
                        descriptionText = "${device.displayName} ${name} was set to ${value}${unit}"
                    }
                    state.lastHue = descMap.value
                    break
                case 1: //sat
                    value = Math.round(rawValue / 254 * 100)
                    name = "saturation"
                    unit = "%"
                    if (device.currentValue("${name}") && value == device.currentValue("${name}").toInteger()){
                        descriptionText = "${device.displayName} ${name} is ${value}${unit}"
                    } else {
                        descriptionText = "${device.displayName} ${name} was set to ${value}${unit}"
                    }
                    state.lastSaturation = descMap.value
                    break
                case 7:	//ct
                    value = (1000000 / rawValue).toInteger()
                    name = "colorTemperature"
                    unit = "°K"
                    if (device.currentValue("${name}") && value == device.currentValue("${name}").toInteger()){
                        descriptionText = "${device.displayName} ${name} is ${value}${unit}"
                    } else {
                        descriptionText = "${device.displayName} ${name} was set to ${value}${unit}"
                    }
                    state.lastCT = descMap.value
                    break
                case 8: //cm
                    if (rawValue == 2) {		//ColorTemperature
                        setGenericTempName(device.currentValue("colorTemperature"))
                    } else if (rawValue == 0){	//HSV
                        setGenericName(device.currentValue("hue"))
                    }
                    value = rawValue == 2 ? "CT" : "RGB"
                    name = "colorMode"
                    descriptionText = "${device.displayName} ${name} is ${value}"
                    break
            }
            break
    }
    if (logEnable) log.debug "evt- rawValue:${rawValue}, value: ${value}, descT: ${descriptionText}"
    if (descriptionText){
        if (txtEnable) log.info "${descriptionText}"
        sendEvent(name:name,value:value,descriptionText:descriptionText, unit: unit)
    }
}

def startLevelChange(direction){
    def upDown = direction == "down" ? 1 : 0
    def unitsPerSecond = 100
    return "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 1 { 0x${intTo8bitUnsignedHex(upDown)} 0x${intTo16bitUnsignedHex(unitsPerSecond)} }"
}

def stopLevelChange(){
    return [
            "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 3 {}}","delay 200",
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0 {}"
    ]
}

def on() {
    def cmd = [
            "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 1 {}",
            "delay 1000",
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}"
    ]
    return cmd
}

def off() {
    def cmd = [
            "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}",
            "delay 1000",
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}"
    ]
    return cmd
}

def refresh() {
    if (logEnable) log.debug "refresh"
    return  [
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}","delay 200",  //light state
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0 {}","delay 200",  //light level
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0000 {}","delay 200", //hue
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0001 {}","delay 200", //sat
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0007 {}","delay 200",	//color temp
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"  		//color mode
    ]

}

def configure() {
    log.warn "configure..."
    runIn(1800,logsOff)
    def cmds = [
            //bindings
            "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0006 {${device.zigbeeId}} {}", "delay 200",
            "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0008 {${device.zigbeeId}} {}", "delay 200",
            "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0300 {${device.zigbeeId}} {}", "delay 200",
            //reporting
            "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 0x10 0 0xFFFF {}","delay 200",
            "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0 0x20 0 0xFFFF {}", "delay 200",
            "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0 0x0000 0 0xFFFF {}", "delay 200",
            "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0 0x0001 0 0xFFFF {}", "delay 200",
            "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0 0x0007 0 0xFFFF {}", "delay 200",
            "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0 0x0008 1 0xFFFE {}", "delay 200",
    ] + refresh()
    return cmds
}

def setGenericTempName(temp){
    if (!temp) return
    def genericName
    def value = temp.toInteger()
    if (value <= 2000) genericName = "Sodium"
    else if (value <= 2100) genericName = "Starlight"
    else if (value < 2400) genericName = "Sunrise"
    else if (value < 2800) genericName = "Incandescent"
    else if (value < 3300) genericName = "Soft White"
    else if (value < 3500) genericName = "Warm White"
    else if (value < 4150) genericName = "Moonlight"
    else if (value <= 5000) genericName = "Horizon"
    else if (value < 5500) genericName = "Daylight"
    else if (value < 6000) genericName = "Electronic"
    else if (value <= 6500) genericName = "Skylight"
    else if (value < 20000) genericName = "Polar"
    def descriptionText = "${device.getDisplayName()} color is ${genericName}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "colorName", value: genericName ,descriptionText: descriptionText)
}

def setGenericName(hue){
    def colorName
    hue = hue.toInteger()
    if (!hiRezHue) hue = (hue * 3.6)
    switch (hue.toInteger()){
        case 0..15: colorName = "Red"
            break
        case 16..45: colorName = "Orange"
            break
        case 46..75: colorName = "Yellow"
            break
        case 76..105: colorName = "Chartreuse"
            break
        case 106..135: colorName = "Green"
            break
        case 136..165: colorName = "Spring"
            break
        case 166..195: colorName = "Cyan"
            break
        case 196..225: colorName = "Azure"
            break
        case 226..255: colorName = "Blue"
            break
        case 256..285: colorName = "Violet"
            break
        case 286..315: colorName = "Magenta"
            break
        case 316..345: colorName = "Rose"
            break
        case 346..360: colorName = "Red"
            break
    }
    if (device.currentValue("saturation") == 0) colorName = "White"
    def descriptionText = "${device.getDisplayName()} color is ${colorName}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "colorName", value: colorName ,descriptionText: descriptionText)
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
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 4 {0x${intTo8bitUnsignedHex(value)} 0x${intTo16bitUnsignedHex(scaledRate)}}",
                "delay ${(rate * 1000) + 400}",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0 {}"
        ]
    } else {
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 4 {0x${intTo8bitUnsignedHex(value)} 0x0100}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0 {}"
        ]
    }
    return cmd
}

def setColor(value){
    if (value.hue == null || value.saturation == null) return

    def rate = value?.rate ? value.rate * 1000 : (transitionTime?.toInteger() ?: 1000)
    def isOn = device.currentValue("switch") == "on"

    def hexSat = zigbee.convertToHexString(Math.round(value.saturation.toInteger() * 254 / 100).toInteger(),2)
    def level
    if (value.level) level = (value.level.toInteger() * 2.55).toInteger()
    def cmd = []
    def hexHue

    //hue is 0..360, value is 0..254 Hue = CurrentHue x 360 / 254
    if (hiRezHue){
        hexHue = zigbee.convertToHexString(Math.round(value.hue / 360 * 254).toInteger(),2)
    } else {
        hexHue = zigbee.convertToHexString(Math.round(value.hue * 254 / 100).toInteger(),2)
    }

    if (isOn){
        if (value.level){
            cmd = [
                    "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x06 {${hexHue} ${hexSat} ${intTo16bitUnsignedHex(rate / 100)}}","delay 200",
                    "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 4 {0x${intTo8bitUnsignedHex(level)} 0x${intTo16bitUnsignedHex(rate / 100)}}",
                    "delay ${rate + 400}",
                    "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0000 {}", "delay 200",
                    "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0001 {}", "delay 200",
                    "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0 {}", "delay 200",
                    "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
            ]
        } else {
            cmd = [
                    "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x06 {${hexHue} ${hexSat} ${intTo16bitUnsignedHex(rate / 100)}}",
                    "delay ${rate + 400}",
                    "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0000 {}", "delay 200",
                    "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0001 {}", "delay 200",
                    "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
            ]
        }
    } else if (colorStaging){
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0000 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0001 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
        ]
    } else if (level){
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 4 {0x${intTo8bitUnsignedHex(level)} 0x0100}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0000 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0001 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
        ]
    } else {
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 1 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0000 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0001 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
        ]
    }
    state.lastSaturation = hexSat
    state.lastHue = hexHue
    return cmd
}

def setHue(value) {
    def hexHue
    def rate = transitionTime?.toInteger() ?: 1000
    def isOn = device.currentValue("switch") == "on"
    //hue is 0..360, value is 0..254 Hue = CurrentHue x 360 / 254
    if (hiRezHue){
        hexHue = zigbee.convertToHexString(Math.round(value / 360 * 254).toInteger(),2)
    } else {
        hexHue = zigbee.convertToHexString(Math.round(value * 254 / 100).toInteger(),2)
    }
    def hexSat = state.lastSaturation
    def cmd = []
    if (isOn){
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x06 {${hexHue} ${hexSat} ${intTo16bitUnsignedHex(rate / 100)}}",
                "delay ${rate + 400}",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0000 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
        ]
    } else if (colorStaging){
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0000 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
        ]
    } else {
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 1 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0000 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
        ]
    }
    state.lastHue = hexHue
    return cmd
}

def setSaturation(value) {
    //saturation is 0.. 100, value is 0..254 Saturation = CurrentSaturation/254
    def rate = transitionTime?.toInteger() ?: 1000
    def cmd = []
    def isOn = device.currentValue("switch") == "on"
    def hexSat = zigbee.convertToHexString(Math.round(value * 254 / 100).toInteger(),2)
    def hexHue = state.lastHue
    if (isOn){
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x06 {${hexHue} ${hexSat} ${intTo16bitUnsignedHex(rate / 100)}}",
                "delay ${rate + 400}",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0001 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
        ]
    } else if (colorStaging){
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0001 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
        ]
    } else {
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x06 {${hexHue} ${hexSat} 0x0100}", "delay 200",
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 1 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0001 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
        ]
    }
    state.lastSaturation = hexSat
    return cmd
}

def setColorTemperature(rawValue) {
    def rate = transitionTime?.toInteger() ?: 1000
    def value = intTo16bitUnsignedHex((1000000/rawValue).toInteger())
    def cmd = []
    def isOn = device.currentValue("switch") == "on"

    if (isOn){
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x000A {${value} ${intTo16bitUnsignedHex(rate / 100)}}",
                "delay ${rate + 400}",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0007 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
        ]
    } else if (colorStaging){
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x000A {${value} 0x0100}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0007 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
        ]
    } else {
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x000A {${value} 0x0100}", "delay 200",
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 1 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0007 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
        ]
    }
    state.lastCT = value
    return cmd
}

def installed() {

}

def intTo16bitUnsignedHex(value) {
    def hexStr = zigbee.convertToHexString(value.toInteger(),4)
    return new String(hexStr.substring(2, 4) + hexStr.substring(0, 2))
}

def intTo8bitUnsignedHex(value) {
    return zigbee.convertToHexString(value.toInteger(), 2)
}
