/*
 	Virtual RGBW Light

    Copyright 2018 -> 2022 Hubitat Inc. All Rights Reserved
    
*/

metadata {
    definition (name: "Virtual RGBW Light", namespace: "hubitat", author: "Mike Maxwell") {
        capability "Actuator"
        capability "Color Control"
        capability "Color Temperature"
        capability "Switch"
        capability "Switch Level"
        capability "Light"
        capability "ColorMode"

    }

    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def updated(){}
def installed() {
    setColorTemperature(2500)
    setColor([hue:50, saturation:100, level:100])
}

void noCommands(cmd) {
    log.trace "Command ${cmd} is not implemented on this device."
}

def parse(String description) { noCommands("parse") }

private eventSend(name,verb,value,unit = ""){
    String descriptionText = "${device.displayName} ${name} ${verb} ${value}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    if (unit != "") sendEvent(name: name, value: value ,descriptionText: descriptionText, unit:unit)
    else  sendEvent(name: name, value: value ,descriptionText: descriptionText)
}

def on() {
    String verb = (device.currentValue("switch") == "on") ? "is" : "was turned"
    eventSend("switch",verb,"on")
}

def off() {
    String verb = (device.currentValue("switch") == "off") ? "is" : "was turned"
    eventSend("switch",verb,"off")
}

def setLevel(value, rate = null) {
    if (value == null) return
    Integer level = limitIntegerRange(value,0,100)
    if (level == 0) {
        off()
        return
    }
    if (device.currentValue("switch") != "on") on()
    String verb = (device.currentValue("level") == level) ? "is" : "was set to"
    eventSend("level",verb,level,"%")
}

def setColor(Map value){
    if (value == null) return
    if (value.hue == null || value.saturation == null) return

    if (device.currentValue("switch") != "on") on()

    if (device.currentValue("colorMode") != "RGB") {
        eventSend("colorMode","is","RGB")
    }
    Integer hue = limitIntegerRange(value.hue,0,100)
    String verb = (device.currentValue("hue") == hue) ? "is" : "was set to"
    eventSend("hue",verb,hue,"%")
    setGenericName(hue)

    Integer sat = limitIntegerRange(value.saturation,0,100)
    verb = (device.currentValue("saturation") == sat) ? "is" : "was set to"
    eventSend("saturation",verb,sat,"%")

    if (value.level) {
        Integer level = limitIntegerRange(value.level,0,100)
        verb = (device.currentValue("level") == level) ? "is" : "was set to"
        eventSend("level",verb,level,"%")
    }
}

def setHue(value) {
    if (value == null) return
    Integer hue = limitIntegerRange(value,0,100)

    if (device.currentValue("switch") != "on") on()
    if (device.currentValue("colorMode") != "RGB") {
        eventSend("colorMode","is","RGB")
    }
    String verb = (device.currentValue("hue") == hue) ? "is" : "was set to"
    eventSend("hue",verb,hue,"%")
    setGenericName(hue)
}

def setSaturation(value) {
    if (value == null) return
    Integer sat = limitIntegerRange(value,0,100)
    if (device.currentValue("switch") != "on") on()
    if (device.currentValue("colorMode") != "RGB") {
        eventSend("colorMode","is","RGB")
    }
    String verb = (device.currentValue("saturation") == sat) ? "is" : "was set to"
    eventSend("saturation",verb,sat,"%")
}

def setColorTemperature(value, level = null, tt = null) {
    if (value == null) return
    if (level) setLevel(level, tt)
    Integer ct = limitIntegerRange(value,2000,6000)
    if (device.currentValue("switch") != "on") on()
    if (device.currentValue("colorMode") != "CT") {
        eventSend("colorMode","is","CT")
    }
    String verb = (device.currentValue("colorTemperature") == ct) ? "is" : "was set to"
    eventSend("colorTemperature",verb,ct,"°K")
    setGenericTempName(ct)
}


def setGenericTempName(value){
    String genericName
    Integer sat = value.toInteger()
    if (sat <= 2000) genericName = "Sodium"
    else if (sat <= 2100) genericName = "Starlight"
    else if (sat < 2400) genericName = "Sunrise"
    else if (sat < 2800) genericName = "Incandescent"
    else if (sat < 3300) genericName = "Soft White"
    else if (sat < 3500) genericName = "Warm White"
    else if (sat < 4150) genericName = "Moonlight"
    else if (sat <= 5000) genericName = "Horizon"
    else if (sat < 5500) genericName = "Daylight"
    else if (sat < 6000) genericName = "Electronic"
    else if (sat <= 6500) genericName = "Skylight"
    else if (sat < 20000) genericName = "Polar"
    eventSend("colorName","is",genericName)
}

def setGenericName(value){
    String colorName
    Integer hue = value.toInteger()
    if (!hiRezHue) hue = (hue * 3.6).toInteger()
    switch (hue){
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
    eventSend("colorName","is",colorName)
}

Integer limitIntegerRange(value,min,max) {
    Integer limit = value.toInteger()
    return (limit < min) ? min : (limit > max) ? max : limit
}
