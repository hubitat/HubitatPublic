import hubitat.helper.ColorUtils
import groovy.transform.Field

metadata {
    definition (name: "LIFX Color Legacy", namespace: "hubitat", author: "Bryan Copeland") {
        capability "SwitchLevel"
        capability "ColorTemperature"
        capability "ColorControl"
        capability "Switch"
        capability "Refresh"
        capability "Actuator"
        capability "SignalStrength"
        capability "Configuration"
        capability "ColorMode"
        capability "Polling"

        command "flash"

    }
    preferences {
        input name: "pollInterval", type: "enum", title: "Poll Interval", defaultValue: 0, options: [0: "Disabled", 5: "5 Minutes", 10: "10 Minutes", 15: "15 Minutes", 30: "30 Minutes", 60: "1 Hour"], submitOnChange: true, width: 8
        input name: "colorStaging", type: "bool", description: "", title: "Enable color pre-staging", defaultValue: false
        input name: "colorTransition", type: "enum", description: "", title: "Transition Time", defaultValue: 0, options: [0: "ASAP", 1: "1 second", 2: "2 seconds", 3: "3 seconds", 4: "4 seconds", 5: "5 seconds", 6: "6 seconds", 7: "7 seconds", 8: "8 seconds", 9: "9 seconds", 10: "10 seconds"]
        input name: "flashRate", type: "enum", title: "Flash rate", options:[[750:"750ms"],[1000:"1s"],[2000:"2s"],[5000:"5s"]], defaultValue: 750
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

@Field static Map<String,Map<String,Integer>> targetColor = [:]

Map<String, Integer> getTargetColorVals() {
    if (!targetColor["${device.id}"]) {
        Map<String, Integer> tempMap = [:]
        tempMap.hue = device.currentValue("hue").toInteger()
        tempMap.colorTemperature = device.currentValue("colorTemperature").toInteger()
        tempMap.level = device.currentValue("level").toInteger()
        tempMap.saturation = device.currentValue("saturation").toInteger()
        targetColor["${device.id}" as String] = tempMap
    }
    return targetColor["${device.id}"]
}

void setTargetColorVals(Map<String, Integer> targetVals) {
    targetColor["${device.id}" as String] = targetVals
}

void logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void configure() {
    if (logEnable) log.debug "configure()"
    List<String> cmds = []
    cmds.add(new hubitat.lifx.commands.GetColor().format())
    cmds.add(new hubitat.lifx.commands.SetLightPower(level: 65535, duration: tt).format())
    cmds.add(new hubitat.lifx.commands.GetHostFirmware().format())
    cmds.add(new hubitat.lifx.commands.GetWifiFirmware().format())
    cmds.add(new hubitat.lifx.commands.GetVersion().format())
    sendToDevice(cmds)
    refresh()
    if (logEnable) runIn(1800,logsOff)
}

void updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    unschedule()
    if (pollInterval) {
        switch (pollInterval.toInteger()) {
            case 5:
                runEvery5Minutes("poll")
                break;
            case 10:
                runEvery10Minutes("poll")
                break;
            case 15:
                runEvery15Minutes("poll")
                break;
            case 30:
                runEvery30Minutes("poll")
                break;
            case 60:
                runEvery60Minutes("poll")
                break;
        }
    }
    if (logEnable) runIn(1800,logsOff)
}

void parse(String description) {
    if (logEnable) log.debug "parse:${description}"
    hubitat.lifx.Command cmd = hubitat.lifx.Lifx.parse(description)
    if (cmd) {
        lifxEvent(cmd)
    }
}

void eventProcess(Map evt) {
    if (device.currentValue(evt.name).toString() != evt.value.toString()) {
        if (!evt.descriptionText) {
            if (evt.unit != null) {
                evt.descriptionText = "${device.displayName} ${evt.name} is ${evt.value}${evt.unit}"
            } else {
                evt.descriptionText = "${device.displayName} ${evt.name} is ${evt.value}"
            }
        }
        sendEvent(evt)
        if (txtEnable) log.info evt.descriptionText
    }
}

void lifxEvent(hubitat.lifx.Command cmd) {
    if (logEnable) log.debug "Unhandled Command: ${cmd}"
}

void refresh() {
    if (logEnable) log.debug "refresh()"
    // manual refresh - clear target vals
    targetColor.remove("${device.id}")
    List<String> cmds = []
    cmds.add(new hubitat.lifx.commands.GetColor().format())
    cmds.add(new hubitat.lifx.commands.GetWifiInfo().format())
    sendToDevice(cmds)
}

void privateRefresh() {
    // rapid commands have stopped - refresh target vals from new state
    targetColor.remove("${device.id}")
    sendToDevice(new hubitat.lifx.commands.GetColor().format())
}

void poll() {
    privateRefresh()
}

void flash() {
    if (logEnable) log.debug "flash()"
    String descriptionText = "${device.getDisplayName()} was set to flash with a rate of ${flashRate ?: 750} milliseconds"
    if (txtEnable) log.info "${descriptionText}"
    state.flashing = true
    flashOn()
}

void flashOn() {
    if (!state.flashing) return
    runInMillis((flashRate ?: 750).toInteger(), flashOff)
    sendToDevice(new hubitat.lifx.commands.SetPower(level: 65535).format())
}

void flashOff() {
    if (!state.flashing) return
    runInMillis((flashRate ?: 750).toInteger(), flashOn)
    sendToDevice(new hubitat.lifx.commands.SetPower(level: 0).format())
}

void on() {
    state.flashing = false
    if (logEnable) log.debug "on()"
    Integer tt = 1000
    if (colorTransition) tt = colorTransition.toInteger() * 1000
    sendToDevice(new hubitat.lifx.commands.SetPower(level: 65535).format())
    runIn((Math.round(tt/1000) + 1), privateRefresh)
}

void off() {
    state.flashing = false
    if (logEnable) log.debug "off()"
    Integer tt = 1000
    if (colorTransition) tt = colorTransition.toInteger() * 1000
    sendToDevice(new hubitat.lifx.commands.SetPower(level: 0).format())
    runIn((Math.round(tt/1000) + 1), privateRefresh)
}

void setLevel(value, duration=null) {
    Map<String, Integer> targetVals = getTargetColorVals()
    targetVals.level = value
    setTargetColorVals(targetVals)
    state.flashing = false
    List<String> cmds = []
    if (logEnable) log.debug "setLevel(${value})"
    Integer tt = 1000
    if (duration != null) {
        tt = duration * 1000
    } else {
        if (colorTransition) tt = colorTransition.toInteger() * 1000
    }
    cmds.add(new hubitat.lifx.commands.SetColor(hue: Math.round(targetVals.hue * 655.35), saturation: Math.round(targetVals.saturation * 655.35), brightness: Math.round(targetVals.level * 655.35), kelvin: targetVals.colorTemperature, duration: tt).format())
    if (device.currentValue("switch") != "on") {
        cmds.add(new hubitat.lifx.commands.SetPower(level: 65535).format())
    }
    sendToDevice(cmds)
    runIn((Math.round(tt/1000) + 1), privateRefresh)
}

void setHue(value) {
    Map<String, Integer> targetVals = getTargetColorVals()
    targetVals.hue = value
    targetVals.saturation = 100
    setTargetColorVals(targetVals)
    state.flashing = false
    List<String> cmds = []
    if (logEnable) log.debug "setHue(${value})"
    Integer tt = 1000
    if (colorTransition) tt = colorTransition.toInteger() * 1000
    cmds.add(new hubitat.lifx.commands.SetColor(hue: Math.round(targetVals.hue * 655.35), saturation: Math.round(targetVals.saturation * 655.35), brightness: Math.round(targetVals.level * 655.35), kelvin: targetVals.colorTemperature, duration: tt).format())
    if (!colorStaging && (device.currentValue("switch") != "on")) {
        cmds.add(new hubitat.lifx.commands.SetPower(level: 65535).format())
    }
    sendToDevice(cmds)
    runIn((Math.round(tt/1000) + 1), privateRefresh)
}

void setSaturation(value) {
    Map<String, Integer> targetVals = getTargetColorVals()
    targetVals.saturation = value
    setTargetColorVals(targetVals)
    state.flashing = false
    List<String> cmds = []
    if (logEnable) log.debug "setSaturation(${value})"
    Integer tt = 1000
    if (colorTransition) tt = colorTransition.toInteger() * 1000
    cmds.add(new hubitat.lifx.commands.SetColor(hue: Math.round(targetVals.hue * 655.35), saturation: Math.round(targetVals.saturation * 655.35), brightness: Math.round(targetVals.level * 655.35), kelvin: targetVals.colorTemperature, duration: tt).format())
    if (!colorStaging && (device.currentValue("switch") != "on")) {
        cmds.add(new hubitat.lifx.commands.SetPower(level: 65535).format())
    }
    sendToDevice(cmds)
    runIn((Math.round(tt/1000) + 1), privateRefresh)
}

void setColor(value) {
    Map<String, Integer> targetVals = getTargetColorVals()
    state.flashing = false
    List<String> cmds = []
    if (logEnable) log.debug "setColor(${value})"
    Integer tt = 1000
    if (colorTransition) tt = colorTransition.toInteger() * 1000
    if (value.hue == null || value.saturation == null) return
    if (value.level == null) value.level=100
    targetVals.saturation = value.saturation
    targetVals.hue = value.hue
    targetVals.level = value.level
    setTargetColorVals(targetVals)
    cmds.add(new hubitat.lifx.commands.SetColor(hue: Math.round(targetVals.hue * 655.35), saturation: Math.round(targetVals.saturation * 655.35), brightness: Math.round(targetVals.level * 655.35), kelvin: targetVals.colorTemperature, duration: tt).format())
    if (!colorStaging && (device.currentValue("switch") != "on")) {
        cmds.add(new hubitat.lifx.commands.SetPower(level: 65535).format())
    }
    sendToDevice(cmds)
    runIn((Math.round(tt/1000) + 1), privateRefresh)
}

void setColorTemperature(Number temp, Number level=null, Number transitionTime=null) {
    Map<String, Integer> targetVals = getTargetColorVals()
    state.flashing = false
    List<String> cmds = []
    Integer tt = 1000
    if (transitionTime == null) {
        if (colorTransition) tt = colorTransition.toInteger() * 1000
    } else {
        tt = transitionTime * 1000
    }
    if (logEnable) log.debug "setColorTemperature(${temp}, ${level}, ${transitionTime})"
    if (level == null) level = targetVals.level
    targetVals.colorTemperature = temp.toInteger()
    targetVals.saturation = 0
    targetVals.level = level.toInteger()
    setTargetColorVals(targetVals)
    cmds.add(new hubitat.lifx.commands.SetColor(hue: Math.round(targetVals.hue * 655.35), saturation: Math.round(targetVals.saturation * 655.35), brightness: Math.round(targetVals.level * 655.35), kelvin: targetVals.colorTemperature, duration: tt).format())
    if (!colorStaging && (device.currentValue("switch") != "on")) {
        cmds.add(new hubitat.lifx.commands.SetPower(level: 65535).format())
    }
    sendToDevice(cmds)
    runIn((Math.round(tt/1000) + 1), privateRefresh)
}

void lifxEvent(hubitat.lifx.commands.StateWifiFirmware cmd) {
    if (logEnable) log.debug "${cmd}"
    Double fwVersion = cmd.versionMajor + (cmd.versionMinor / 100)
    device.updateDataValue("wifiFirmware", "${fwVersion}")
}

void lifxEvent(hubitat.lifx.commands.StateHostFirmware cmd) {
    if (logEnable) log.debug "${cmd}"
    Double fwVersion = cmd.versionMajor + (cmd.versionMinor / 100)
    device.updateDataValue("hostFirmware", "${fwVersion}")
}

void lifxEvent(hubitat.lifx.commands.StateVersion cmd) {
    if (logEnable) log.debug "${cmd}"
    device.updateDataValue("model: ", "${cmd.product}")
    Map<String, Object> lifxProduct = hubitat.helper.Lifx.lifxProducts[cmd.product]
    if (lifxProduct != null) {
        device.updateDataValue("modelName", lifxProduct.name)
    }
}

void lifxEvent(hubitat.lifx.commands.StateWifiInfo cmd) {
    Integer rssi = Math.floor(10 * Math.log10(cmd.signal.doubleValue()) + 0.5).toInteger()
    eventProcess(name: "rssi", value: rssi, unit: "dBm")
}

void lifxEvent(hubitat.lifx.commands.LightState cmd) {
    if (logEnable) log.debug cmd.toString()
    if (cmd.saturation > 0) {
        eventProcess(name: "colorMode", value: "RGB")
        setGenericName(Math.round(cmd.hue / 655.35))
    } else {
        eventProcess(name: "colorMode", value: "CT")
        setGenericTempName(cmd.kelvin)
    }
    eventProcess(name: "hue", value: Math.round(cmd.hue / 655.35))
    eventProcess(name: "saturation", value: Math.round(cmd.saturation / 655.35), unit: "%")
    eventProcess(name: "colorTemperature", value: cmd.kelvin, unit: "K")
    eventProcess(name: "level", value: Math.round(cmd.brightness / 655.35), unit: "%")
    eventProcess(name: "color", value: ColorUtils.rgbToHEX(ColorUtils.hsvToRGB([Math.round(cmd.hue / 655.35), Math.round(cmd.saturation / 655.35), Math.round(cmd.brightness / 655.35)])))
}

void lifxEvent(hubitat.lifx.commands.StateLightPower cmd) {
    if (logEnable) log.debug cmd.toString()
}

void lifxEvent(hubitat.lifx.commands.StatePower cmd) {
    if (logEnable) log.debug cmd.toString()
    if (cmd.level > 0) {
        eventProcess(name: "switch", value: "on")
    } else {
        eventProcess(name: "switch", value: "off")
    }
}

private void setGenericTempName(temp){
    if (!temp) return
    String genericName
    int value = temp.toInteger()
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
    String descriptionText = "${device.getDisplayName()} color is ${genericName}"
    eventProcess(name: "colorName", value: genericName ,descriptionText: descriptionText)
}

private void setGenericName(hue){
    String colorName
    hue = hue.toInteger()
    hue = (hue * 3.6)
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
    String descriptionText = "${device.getDisplayName()} color is ${colorName}"
    eventProcess(name: "colorName", value: colorName ,descriptionText: descriptionText)
}

void sendToDevice(List<String> cmds, Long delay = 300) {
    sendHubCommand(new hubitat.device.HubMultiAction(commands(cmds, delay), hubitat.device.Protocol.LIFX))
}

void sendToDevice(String cmd, Long delay = 300) {
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.LIFX))
}

List<String> commands(List<String> cmds, Long delay = 300) {
    return delayBetween(cmds.collect { it }, delay)
}
