/*
	Advanced Zigbee CT Bulb

	Copyright 2016 -> 2021 Hubitat Inc.  All Rights Reserved
	2021-05-27 2.2.8 maxwell
		-implement three param setColorTemperature
	2021-02-11 2.2.6 maxwell
	    -add 1 percent minimum level option
	    -add command logging
	2021-01-07 2.2.5 maxwell
		-initial pub

*/

import groovy.transform.Field

//transitionTime options
@Field static Map ttOpts = [
        defaultValue: "0A00"
        ,defaultText: "1s"
        ,options:["0000":"ASAP","0500":"500ms","0A00":"1s","0F00":"1.5s","1400":"2s","3200":"5s"]
]

//transitionTime options
@Field static Map ttOnOffOpts = [
        defaultValue: "0A00"
        ,defaultText: "1s"
        ,options:["0000":"ASAP","0500":"500ms","0A00":"1s","0F00":"1.5s","1400":"2s","3200":"5s"]
]

@Field static Map ttOnOff2Opts = [
        defaultValue: "FFFF"
        ,defaultText: "Use On transition time"
        ,options:["0000":"ASAP","FFFF":"Use On transition time","0500":"500ms","0A00":"1s","0F00":"1.5s","1400":"2s","3200":"5s"]
]

@Field static Map ttCTOpts = [
        defaultValue: "FFFF"
        ,defaultText: "Use Level transition time"
        ,options:["0000":"ASAP","FFFF":"Use Level transition time","0500":"500ms","0A00":"1s","0F00":"1.5s","1400":"2s","3200":"5s"]
]

//startLevelChangeRate options
@Field static Map slcOpts = [
        defaultValue: 64
        ,defaultText: "Fast"
        ,options:[10:"Slow",40:"Medium",64:"Fast"]
]

//power restore options
@Field static Map pwrRstOpts = [
        defaultValue: "on"
        ,defaultText: "On"
        ,options:["on":"On","off":"Off","last":"Last state"]
]

@Field static Map minimumOpts = [
        defaultValue: "0D"
        ,defaultText: "5%"
        ,options:["03":"1%","0D":"5%","19":"10%","26":"15%","33":"20%","3F":"25%","4C":"30%"]
]

@Field static Map colorTempName = [
        2001:"Sodium"
        ,2101:"Starlight"
        ,2400:"Sunrise"
        ,2800:"Incandescent"
        ,3300:"Soft White"
        ,3500:"Warm White"
        ,4150:"Moonlight"
        ,5001:"Horizon"
        ,5500:"Daylight"
        ,6000:"Electronic"
        ,6501:"Skylight"
        ,20000:"Polar"
]

metadata {
    definition (name: "Advanced Zigbee CT Bulb", namespace: "hubitat", author: "Mike Maxwell") {
        capability "Actuator"
        capability "Switch"
        capability "SwitchLevel"
        capability "ChangeLevel"
        capability "Bulb"
        capability "Configuration"
        capability "Color Temperature"
        //capability "Level Preset"
        //capability "Color Preset"

        command "flash"
        command "presetLevel", [[name:"Preset Level*", type:"NUMBER", description:"Preset a level (1..100)", constraints:["NUMBER"]]] //part of future capability Level Preset 1..100

        command "updateFirmware"
        /*
             fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B04,FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY BR Tunable White", deviceJoinName: "OSRAM LIGHTIFY LED Flood BR30 Tunable White"
            fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B04,FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY RT Tunable White", deviceJoinName: "OSRAM LIGHTIFY LED Recessed Kit RT 5/6 Tunable White"
            fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B04,FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "Classic A60 TW", deviceJoinName: "OSRAM LIGHTIFY LED Tunable White 60W"
            fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B04,FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY A19 Tunable White", deviceJoinName: "OSRAM LIGHTIFY LED Tunable White 60W"
            fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B04,FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "Classic B40 TW - LIGHTIFY", deviceJoinName: "OSRAM LIGHTIFY Classic B40 Tunable White"
        */
        //removed from generic
        fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LTW003", deviceJoinName: "Philips Ambiance White"
        fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LTW004", deviceJoinName: "Philips Ambiance White"
        fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LTW011", deviceJoinName: "Philips Ambiance White"
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,0B05,1000", outClusters: "0005,0019,0020,1000", manufacturer: "IKEA of Sweden", model: "TRADFRI bulb E14 WS opal 400lm", deviceJoinName: "TRADFRI bulb E14"
        fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LTW012", deviceJoinName: "Philips White Ambiance"
        fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,1000,0300", outClusters: "0019", manufacturer: "Philips", model: "LTW012", deviceJoinName: "Philips White Ambiance"
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300,FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY Under Cabinet TW", deviceJoinName: "LIGHTIFY Under Cabinet TW"
        fingerprint profileId: "C05E", inClusters: "0000,0004,0003,0006,0008,0005,0300,0019", outClusters: "0000,0004,0003,0006,0008,0005,0300,0019", manufacturer: "Trust International B.V.", model: "ZLL-ColorTempera", deviceJoinName: "AduroSmart ERIA White Tunable"
        fingerprint profileId: "C05E", inClusters: "0000,0004,0003,0006,0008,0005,0300,FFFF,0019", outClusters: "0000,0004,0003,0006,0008,0005,0300,0019", manufacturer: "AduroSmart Eria", model: "ZLL-ColorTemperature", deviceJoinName: "AduroSmart ERIA White Tunable"
        fingerprint profileId: "C05E", endpointId:"01", inClusters:"0000,0004,0003,0006,0008,0005,0300,FFFF,FFFF,0019", outClusters:"0000,0004,0003,0006,0008,0005,0300,0019", model:"ZLL-ColorTempera", manufacturer:"Trust International B.V.", deviceJoinName: "AduroSmart ERIA White Tunable"
        //new in 2.2.5
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0004,0003,0006,0008,0005,0300,FFFF,FFFF,1000", outClusters:"0019", model:"TWBulb51AU", manufacturer:"Aurora", deviceJoinName: "Aurora smart tunable GLS"
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0004,0003,0006,0008,0005,0300,FFFF,FFFF,1000", outClusters:"0019", model:"TWCLBulb50AU", manufacturer:"Aurora", deviceJoinName: "Aurora smart tunable candle lamp"
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0008,0300,0B05,1000,FC82", outClusters:"000A,0019", model:"ZBT-CCTLight-GLS0108", manufacturer:"lk", deviceJoinName: "Linkind tunable white"
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0008,0300,0B05,1000,FC82", outClusters:"000A,0019", model:"TWGU10Bulb50AU", manufacturer:"Aurora", deviceJoinName: "Aurora smart tunable GU10"
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0008,0300", outClusters:"0019", model:"TWMPROZXBulb50AU", manufacturer:"Aurora",  deviceJoinName: "Aurora smart tunable LED"

    }
    preferences {
        input(name:"transitionTime", type:"enum", title:"Level transition time (default:${ttOpts.defaultText})", options:ttOpts.options, defaultValue:ttOpts.defaultValue)
        input(name:"startLevelChangeRate", type:"enum", title: "Start level change rate (default:${slcOpts.defaultText})", options:slcOpts.options, defaultValue:slcOpts.defaultValue)
        input(name:"onTransitionTime", type:"enum", title:"On transition time (default:${ttOnOffOpts.defaultText})", options:ttOnOffOpts.options, defaultValue:ttOnOffOpts.defaultValue)
        input(name:"offTransitionTime", type:"enum", title:"Off transition time (default:${ttOnOff2Opts.defaultText})", options:ttOnOff2Opts.options, defaultValue:ttOnOff2Opts.defaultValue)
        input(name:"ctTransitionTime", type:"enum", title:"Color Temperature transition time (default:${ttCTOpts.defaultText})", options:ttCTOpts.options, defaultValue:ttCTOpts.defaultValue)
        input(name:"minimumLevel", type:"enum",title: "Minimum level, (default:${minimumOpts.defaultText})", options:minimumOpts.options, defaultValue:minimumOpts.defaultValue)
        input(name:"powerRestore", type:"enum", title:"Power restore state (default:${pwrRstOpts.defaultText})", options:pwrRstOpts.options, defaultValue:pwrRstOpts.defaultValue)
        input(name:"logEnable", type:"bool", title:"Enable debug logging", defaultValue:false)
        input(name:"txtEnable", type:"bool", title:"Enable descriptionText logging", defaultValue:true)
    }
}

List<String> updateFirmware() {
    if (getDataValue("manufacturer") == "Philips") return zigbee.updateFirmware(manufacturer:0x100B)
    else return zigbee.updateFirmware()
}

//parsers
void parse(String description) {
    Map descMap = zigbee.parseDescriptionAsMap(description)
    if (logEnable) log.debug "descMap:${descMap}"
    String status

    if (descMap.clusterId != null && descMap.profileId == "0104") {
        if (descMap.isClusterSpecific == false) { //global commands
            switch (descMap.command) {
                case "0B" ://command response
                    String clusterCmd = descMap.data[0]
                    status =  descMap.data[1]
                    switch (descMap.clusterId) {
                        case "0300" :
                            if (status == "00" && state.checkPhase == 0) {
                                switch (clusterCmd) {
                                    case "0A" : //move to color temp
                                        sendColorTempEvent(state.ct.requested)
                                        break
                                    default :
                                        if (logEnable) log.trace "skipped color command:${clusterCmd}"
                                }
                            } else {
                                if (logEnable) log.trace "invalid color command ${descMap}"
                            }
                            break
                        case "0006" :
                            if (state.checkPhase == 1 && clusterCmd == "01") {
                                runOptionTest(4)
                            } else {
                                switch (clusterCmd) {
                                    case ["00","01"] : //on/off
                                        sendSwitchEvent(clusterCmd)
                                        break
                                    default :
                                        if (logEnable) log.debug "skipped clusterCmd:${clusterCmd}, status:${status}"
                                        break
                                }
                            }
                            break
                        case "0008" :
                            switch (clusterCmd) {
                                case "01" : //startLevelChange
                                    //do nothing
                                    break
                                case "03" : //stopLevelChange
                                    getLevel()
                                    break
                                case "04" : //move with on off
                                    sendLevelEvent(state.hexLevel.requested)
                                    break
                                case "00" : //move
                                    getLevel()
                                    break
                            }
                            break
                    }
                    if (status == "82") {
                        if (logEnable) log.info "unsupported general command cluster:${descMap.clusterId}, command:${clusterCmd}"
                    }
                    break
                case "04" : //write attribute response
                    if (logEnable) log.debug "writeAttributeResponse- status:${descMap.data[0]}"
                    break
                default :
                    if (logEnable) log.info "skipped global command cluster:${descMap.clusterId}, command:${descMap.command}, data:${descMap.data}"
            }
        } else { //cluster specific
            switch (descMap.clusterId) {
                case "0004": //group
                    processGroupCommand(descMap)
                    break
                default :
                    if (logEnable) log.info "skipped cluster specific command cluster:${descMap.clusterId}, command:${descMap.command}, data:${descMap.data}"
            }
        }
        return
    } else if (descMap.profileId == "0000") { //zdo
        switch (descMap.clusterId) {
            case "8005" : //endpoint response
                break
            case "8004" : //simple descriptor response
                break
            case "8034" : //leave response
                break
            case "8021" : //bind response
                break
            case "8022" : //unbind request
                break
            case "0013" : //"device announce"
                if (logEnable) log.trace "device announce..."
                String currentState = device.currentValue("switch") ?: "on"
                String opt = powerRestore ?: pwrRstOpts.defaultValue
                List<String> cmds = configAttributeReporting()
                if ((opt == "last" && currentState == "off") || (opt == "off")) {
                    cmds.addAll(zigbee.off(0))
                }
                sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
                String currentAddress = descMap.data[2] + descMap.data[1]
                if (currentAddress != state.lastAddress) {
                    if (txtEnable) log.info "address changed! new:${currentAddress}, old:${state.lastAddress}, checking group membership status..."
                    state.lastAddress = currentAddress
                    runIn(4,getGroups)
                }
                break
            default :
                if (logEnable) log.debug "skipped zdo:${descMap.clusterId}"
        }
        return
    }

    List<Map> additionalAttributes = []
    additionalAttributes.add(["attrId":descMap.attrId, "value":descMap.value, "encoding":descMap.encoding])
    if (descMap.additionalAttrs) additionalAttributes.addAll(descMap.additionalAttrs)
    parseAttributes(descMap.cluster, descMap.endpoint, additionalAttributes, descMap.command)
}

private void parseAttributes(String cluster, String endPoint, List<Map> additionalAttributes, String command){
    additionalAttributes.each{
        switch (cluster) {
            case "0006" :
                if (it.attrId == "0000") { //switch
                    sendSwitchEvent(it.value)
                }
                break
            case "0008" :
                if (it.attrId == "0000") { //current level
                    sendLevelEvent(it.value)
                }
                break
            case "0000" :
                if (it.attrId == "4000") { //software build
                    updateDataValue("softwareBuild",it.value ?: "unknown")
                }
                break
            case "0300" :
                if (it.attrId == "0007") { //color temperature
                    if (state.checkPhase == 4) {
                        state.ct.min = myredHexToCt(zigbee.swapOctets(it.value))
                        runOptionTest(5)
                    } else if (state.checkPhase == 5) {
                        state.ct.max = myredHexToCt(zigbee.swapOctets(it.value))
                        runOptionTest(10)
                    }
                    sendColorTempEvent(zigbee.swapOctets(it.value))
                } //else log.trace "skipped color, attribute:${it.attrId}, value:${it.value}"
                break
            default :
                if (logEnable) {
                    String respType = (command == "0A") ? "reportResponse" : "readAttributeResponse"
                    log.debug "skipped:${cluster}:${it.attrId}, value:${it.value}, encoding:${it.encoding}, respType:${respType}"
                }
        }
    }
}

private void processGroupCommand(Map descMap) {
    String status = descMap.data[0]
    String group
    if (state.groups == null) state.groups = []

    switch (descMap.command){
        case "00" : //add group response
            if (status in ["00","8A"]) {
                group = descMap.data[1] + descMap.data[2]
                if (group in state.groups) {
                    if (txtEnable) log.info "group membership refreshed"
                } else {
                    state.groups.add(group)
                    if (txtEnable) log.info "group membership added"
                }
            } else {
                log.warn "${device.displayName}'s group table is full, unable to add group..."
            }
            break
        case "03" : //remove group response
            group = descMap.data[1] + descMap.data[2]
            state.groups.remove(group)
            if (txtEnable) log.info "group membership removed"
            break
        case "02" : //group membership response
            Integer groupCount = hexStrToUnsignedInt(descMap.data[1])
            if (groupCount == 0 && state.groups != []) {
                List<String> cmds = []
                state.groups.each {
                    cmds.addAll(zigbee.command(0x0004,0x00,[:],0,"${it} 00"))
                    if (txtEnable) log.warn "update group:${it} on device"
                }
                sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(cmds,500), hubitat.device.Protocol.ZIGBEE))
            } else {
                //get groups and update state...
                Integer crntByte = 0
                for (int i = 0; i < groupCount; i++) {
                    crntByte = (i * 2) + 2
                    group = descMap.data[crntByte] + descMap.data[crntByte + 1]
                    if ( !(group in state.groups) ) {
                        state.groups.add(group)
                        if (txtEnable) log.info "group added to local list"
                    } else {
                        if (txtEnable) log.debug "group already exists in local list..."
                    }
                }
            }
            break
        default :
            if (txtEnable) log.warn "skipped group command:${descMap}"
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

private void sendLevelEvent(String rawValue, Boolean presetLevel = false) {
    if (rawValue in ["00","01"]) {
        if (rawValue == "00") sendSwitchEvent("00")
        else {
            String minLevel = minimumLevel ?: minimumOpts.defaultValue
            state.hexLevel.requested = minLevel
            sendHubCommand(new hubitat.device.HubMultiAction(zigbee.command(0x0008,0x04,[:],0,"${minLevel} 03 00"),hubitat.device.Protocol.ZIGBEE))
        }
        return
    } else if (!presetLevel && device.currentValue("switch") == "off") {
        sendSwitchEvent("01")
    }
    if (rawValue == state.hexLevel.current) return
    state.hexLevel.current = rawValue
    Integer value = Math.round(hexStrToUnsignedInt(rawValue) / 2.55)
    String descriptionText = "${device.displayName} level was set to ${value}%"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"level", value:value, descriptionText:descriptionText, unit: "%")
}

private void sendCTNameEvent(temp){
    String genericName = colorTempName.find{k , v -> temp < k}.value
    if (genericName == device.currentValue("colorName")) return
    String descriptionText = "${device.displayName} color is ${genericName}"
    if (txtEnable) log.info descriptionText
    sendEvent(name: "colorName", value: genericName ,descriptionText: descriptionText)
}

private void sendColorTempEvent(String rawValue, Boolean presetColor = false) {
    if (!presetColor && device.currentValue("switch") == "off") sendSwitchEvent("01")
    Integer value = myredHexToCt(rawValue)
    sendCTNameEvent(value)
    if (rawValue == state.ct.current) return
    state.ct.current = rawValue
    String descriptionText = "${device.displayName} color temperature was set to ${value}°K"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"colorTemperature", value:value, descriptionText:descriptionText, unit: "°K")
}

//capability commands
List<String> on() {
    if (logEnable) log.debug "on()"
    state.hexLevel.requested = state.hexLevel.current
    List<String> cmds = []
    if (state.flashing) cmds.addAll(flash())
    cmds.addAll(zigbee.command(0x0008,0x04,[:],0,"${state.hexLevel.current} ${state.tt.on}"))
    if (state.ct.requested != "0" && state.ct.current != state.ct.requested) cmds.addAll(setColorTemperaturePrivate(myredHexToCt(state.ct.current), false))
    return delayBetween(cmds,500)
}

List<String> off() {
    if (logEnable) log.debug "off()"
    state.hexLevel.requested = "00"
    List<String> cmds = []
    if (state.flashing) {
        cmds.addAll(flash())
        cmds.addAll(zigbee.off(0))
    } else {
        cmds.addAll(zigbee.command(0x0008,0x04,[:],0,"00 ${state.tt.off}"))
    }
    return delayBetween(cmds,500)
}

List<String> flash() {
    if (logEnable) log.debug "flash()"
    List<String> cmds = []
    if (state.flashing) {
        state.flashing = false
        cmds.addAll(zigbee.command(0x0003, 0x00, [:], 0, "00 00"))
    } else {
        state.flashing = true
        if (device.currentValue("switch") != "on") {
            cmds.addAll(zigbee.on(0))
        }
        cmds.addAll(zigbee.command(0x0003, 0x00, [:], 0, "FF FF"))
    }
    return delayBetween(cmds,1000)
}

List<String> setLevel(Object value) {
    if (logEnable) "setLevel(${value})"
    return setLevelPrivate(value,false)
}

List<String> setLevel(Object value, Object rate) {
    if (logEnable) log.debug "setLevel(${value}, ${rate})"
    return setLevelPrivate(value, rate, false)
}

List<String> startLevelChange(String direction){
    if (logEnable) log.debug "startLevelChange(${direction})"
    if (device.currentValue("switch") == "off") return
    String upDown = "00"
    state.hexLevel.requested = "FE"
    if (direction == "down") {
        upDown = "01"
        state.hexLevel.requested = minimumLevel ?: minimumOpts.defaultValue
    }
    String unitsPerSecond = startLevelChangeRate ?: slcOpts.defaultValue
    Integer lps = Math.round(hexStrToUnsignedInt(unitsPerSecond) / 2.55)
    Integer crnt = device.currentValue("level")
    Integer delta = (direction == "down") ?  crnt : 100 - crnt
    runIn((delta/lps).toInteger() + 3 ,getLevel)
    return zigbee.command(0x0008,0x01,[:],0,"${upDown} ${unitsPerSecond}")
}

List<String> stopLevelChange(){
    if (logEnable) log.debug "stopLevelChange()"
    if (device.currentValue("switch") == "off") return
    return zigbee.command(0x0008,0x03,[:],0)
}

List<String> presetLevel(Object value) {
    if (logEnable) log.debug "presetLevel(${value})"
    return setLevelPrivate(value, true)
}

List<String> setColorTemperature(colorTemperature, level = null, tt = null) {
    if (logEnable) log.debug "setColorTemperature(${colorTemperature}, ${level}, ${tt})"
    if (colorTemperature == null) return
    List<String> cmds = []
    if (level != null) {
        cmds.addAll(setLevelPrivate(level, tt, false))
    }
    cmds.addAll(setColorTemperaturePrivate(colorTemperature, tt, false))
    return delayBetween(cmds,400)
}

List<String> presetColor(Map colorMap) {
    List<String> cmds = []
    if (colorMap.level != null) {
        cmds.addAll(setLevelPrivate(colorMap.level, true))
    }
    if (colorMap.colorTemperature != null) {
        cmds.addAll(setColorTemperaturePrivate(colorMap.colorTemperature, true))
    }
    if (cmds) return delayBetween(cmds,400)
}

List<String> refresh() {
    if (logEnable) log.debug "refresh()"
    List<String> cmds = zigbee.readAttribute(0x0006, 0x0000, [:],0)
    cmds.addAll(zigbee.readAttribute(0x0300,[0x0003,0x0004,0x0007],[:],0))
    return delayBetween(cmds,400)
}

List<String> configure() {
    log.warn "configure..."
    runIn(3,runOptionTest)
    return configAttributeReporting()
}

//lifecycle commands
List<String> updated(){
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
    return configAttributeReporting()
}

private List<String> setLevelPrivate(Object value, Object rate = null, Boolean presetLevel) {
    Integer level = value.toInteger()
    if (level == 0 && device.currentValue("switch") == "off") return []
    Integer minLevel =  Math.round(hexStrToUnsignedInt((minimumLevel ?: minimumOpts.defaultValue))  / 2.55)
    String hexLevel = intToHexStr((level * 2.55).toInteger())
    if (level <= minLevel) {
        hexLevel = intToHexStr((minLevel * 2.55).toInteger())
    } else if (level > 99) hexLevel = "FE"
    if (presetLevel == true && device.currentValue("switch") == "off") {
        sendLevelEvent(hexLevel, true)
        return []
    } else {
        List<String> cmds = []
        if (state.flashing) cmds.addAll(flash())
        state.hexLevel.requested = hexLevel
        if (rate == null) {
            rate = state.tt.level
        } else {
            rate = zigbee.swapOctets(intToHexStr((rate.toBigDecimal() * 10).toInteger(),2))
        }
        if (level == 0) {
            state.hexLevel.requested = "00"
            cmds.addAll(zigbee.command(0x0008,0x04,[:],0,"01 ${rate}")) //move to level with on off, same as setLevel
            runInMillis(hexStrToUnsignedInt(zigbee.swapOctets(rate)) * 100 + 2000, switchOff)
        } else {
            state.hexLevel.requested = hexLevel
            cmds.addAll(zigbee.command(0x0008,0x04,[:],0,"${hexLevel} ${rate}")) //move to level with on off, same as setLevel
        }
        return cmds
    }
}

private List<String> setColorTemperaturePrivate(Object value, Object rate = null, Boolean presetCT) {
    if (rate == null) {
        rate = state.tt.colorTemperature
    } else {
        rate = zigbee.swapOctets(intToHexStr((rate.toBigDecimal() * 10).toInteger(),2))
    }
    Integer ct = value.toInteger()
    if (ct > state.ct.max) ct = state.ct.max
    else if ((ct < state.ct.min))  ct = state.ct.min
    String hexMyred = ctToMyredHex(ct)
    if (presetCT == true && device.currentValue("switch") == "off") {
        sendColorTempEvent(hexMyred, true)
        return []
    } else {
        List<String> cmds = []
        state.ct.requested = hexMyred
        if (device.currentValue("switch") == "off") {
            cmds.addAll(zigbee.command(0x0008,0x04,[:],0,"${state.hexLevel.current} ${state.tt.level}"))
        }
        if (device.currentValue("colorMode") != "CT") rate = "0000"
        cmds.addAll(zigbee.command(0x0300, 0x0A, [:],0,"${hexMyred} ${rate}"))
        return cmds
    }
}

void switchOff() {
    sendHubCommand(new hubitat.device.HubMultiAction(zigbee.off(0), hubitat.device.Protocol.ZIGBEE))
}

void getLevel() {
    sendHubCommand(new hubitat.device.HubMultiAction(zigbee.readAttribute(0x0008, 0x0000, [:], 0), hubitat.device.Protocol.ZIGBEE))
}

void getGroups() {
    sendHubCommand(new hubitat.device.HubMultiAction(zigbee.command(0x0004,0x02,[:],0,"00"), hubitat.device.Protocol.ZIGBEE))
}

void runOptionTest(Integer phase = 1){
    List<String> cmds = []
    switch (phase) {
        case 1 :
            log.debug "starting options testing..."
            state.hexLevel = ["current":"00","requested":"00"]
            state.ct = ["current":0,"requested":0, "min":0, "max":0]
            state.lastAddress = device.deviceNetworkId
            if (state.groups == null) {
                state.groups = []
            }
            state.xyOnly = false
            device.updateSetting("logEnable",[value:"false",type:"bool"])
            device.updateSetting("txtEnable",[value:"false",type:"bool"])
            cmds.addAll(zigbee.readAttribute(0x0000,0x4000,[:],500)) //get firmware version
            cmds.addAll(zigbee.on(0))
            break
        case 4 : //get min ct
            cmds.addAll(zigbee.command(0x0300, 0x0A, [:],1000,"FFFF 0000"))
            cmds.addAll(zigbee.readAttribute(0x0300, 0x0007, [:],0))
            break
        case 5 : //get max ct
            cmds.addAll(zigbee.command(0x0300, 0x0A, [:],1000,"0000 0000"))
            cmds.addAll(zigbee.readAttribute(0x0300, 0x0007, [:],0))
            break
        case 10 :
            cmds.addAll(zigbee.command(0x0008, 0x04,[:],400,"33 0000"))
            cmds.addAll(refresh())
            cmds.add("delay 400")
            cmds.addAll(setLevelPrivate(20,0,false))
            phase = 0
            if (state.ct.min == state.ct.max) { //bulb won't return limits
                state.ct.min = 2700
                state.ct.max = 6000
            }
            log.debug "finished options testing..."
            device.updateSetting("txtEnable",[value:"true",type:"bool"])
            runIn(3,getGroups)
            break
    }
    state.checkPhase = phase
    if (cmds) sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

private List<String> configAttributeReporting(){
    state.tt = ["level":"0000", "colorTemperature":"0000", "on":"0000", "off":"0000"]
    state.tt.level = settings.transitionTime ?: ttOpts.defaultValue
    state.tt.colorTemperature = ((settings.ctTransitionTime ?: ttCTOpts.defaultValue) != "FFFF") ? settings.ctTransitionTime : state.tt.level
    state.tt.on = settings.onTransitionTime ?: ttOnOffOpts.defaultValue
    state.tt.off = ((settings.offTransitionTime ?: ttOnOff2Opts.defaultValue) != "FFFF") ? settings.offTransitionTime : state.tt.on
    return ["zdo unbind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0300 {${device.zigbeeId}} {}" , "delay 400" ,
            "zdo unbind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0006 {${device.zigbeeId}} {}" , "delay 400" ,
            "zdo unbind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0008 {${device.zigbeeId}} {}"]
}

void logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

String ctToMyredHex(ct) {
    return zigbee.swapOctets(intToHexStr((1000000/ct).toInteger(),2))
}

Integer myredHexToCt(String myred){
    return (1000000 / hexStrToUnsignedInt(zigbee.swapOctets(myred))).toInteger()
}

Integer hex254ToInt100(String value) {
    return Math.round(hexStrToUnsignedInt(value) / 2.54)
}

String int100ToHex254(value) {
    return intToHexStr(Math.round(value * 2.54))
}
