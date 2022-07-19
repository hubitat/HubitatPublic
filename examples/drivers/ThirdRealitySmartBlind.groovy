import hubitat.zigbee.zcl.DataType

metadata {
    definition(name: "ThirdReality Smart Blind", namespace: "mwerline", author: "Third Reality", importUrl: "https://raw.githubusercontent.com/mwerline/hubitat/main/ThirdRealitySmartBlind.groovy") {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        capability "Window Shade"
        capability "Health Check"
        capability "Switch Level"
        capability "Battery"

        command "pause"

       	attribute "lastCheckin", "String"
        attribute "lastOpened", "String"
        attribute "deviceLevel", "Number"

		fingerprint deviceJoinName: "ThirdReality Window Blinds", model: "3RSB015BZ", profileId: "0104", endpointId: 01, inClusters: "0000,0001,0006,0102", outClusters: "0006,0102,0019", manufacturer: "Third Reality, Inc"
    }
}

private getCLUSTER_BATTERY_LEVEL() { 0x0001 }
private getCLUSTER_WINDOW_COVERING() { 0x0102 }
private getCOMMAND_OPEN() { 0x00 }
private getCOMMAND_CLOSE() { 0x01 }
private getCOMMAND_PAUSE() { 0x02 }
private getCOMMAND_GOTO_LIFT_PERCENTAGE() { 0x05 }
private getATTRIBUTE_POSITION_LIFT() { 0x0008 }
private getATTRIBUTE_CURRENT_LEVEL() { 0x0000 }
private getCOMMAND_MOVE_LEVEL_ONOFF() { 0x04 }
private getBATTERY_PERCENTAGE_REMAINING() { 0x0021 }
openLevel = 0
closeLevel = 100

// Utility function to Collect Attributes from event
private List<Map> collectAttributes(Map descMap) {
	List<Map> descMaps = new ArrayList<Map>()

	descMaps.add(descMap)

	if (descMap.additionalAttrs) {
		descMaps.addAll(descMap.additionalAttrs)
	}

	return descMaps
}

// Parse incoming device reports to generate events
def parse(String description) {
    log.debug "Parse report description:- '${description}'."
    def now = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)

    //  Send Event for device heartbeat    
    sendEvent(name: "lastCheckin", value: now)
    
    // Parse Event
    if (description?.startsWith("read attr -")) {
        Map descMap = zigbee.parseDescriptionAsMap(description)
        
        // Zigbee Window Covering Event
        if (descMap?.clusterInt == CLUSTER_WINDOW_COVERING && descMap.value) {
            log.debug "attr: ${descMap?.attrInt}, value: ${descMap?.value}, descValue: ${Integer.parseInt(descMap.value, 16)}, ${device.getDataValue("model")}"

            // Parse Attributes into a List
            List<Map> descMaps = collectAttributes(descMap)
            
            // Get the Current Shade Position
            def liftmap = descMaps.find { it.attrInt == ATTRIBUTE_POSITION_LIFT }
            if (liftmap && liftmap.value) levelEventHandler(zigbee.convertHexToInt(liftmap.value))
        } else if (descMap?.clusterInt == CLUSTER_BATTERY_LEVEL && descMap.value) {
            if(descMap?.value) {
                batteryLevel = Integer.parseInt(descMap.value, 16)
                batteryLevel = convertBatteryLevel(batteryLevel)
                log.debug "attr: '${descMap?.attrInt}', value: '${descMap?.value}', descValue: '${batteryLevel}'."
                sendEvent(name: "battery", value: batteryLevel)
            } else {
                log.debug "failed to parse battery level attr: '${descMap?.attrInt}', value: '${descMap?.value}'."
            }
        }
    }
}

// Convert Battery Level to (0-100 Scale)
def convertBatteryLevel(rawValue) {
    def batteryLevel = rawValue - 50
    batteryLevel = batteryLevel * 100
	batteryLevel = batteryLevel.intdiv(150)
    return batteryLevel
}

// Handle Level Change Reports
def levelEventHandler(currentLevel) {
    def lastLevel = device.currentValue("deviceLevel")
    log.debug "levelEventHandler - currentLevel: '${currentLevel}' lastLevel: '${lastLevel}'."

    if (lastLevel == "undefined" || currentLevel == lastLevel) { 
        // Ignore invalid reports
        log.debug "undefined lastLevel"
        runIn(3, "updateFinalState", [overwrite:true])
    } else {
        setReportedLevel(currentLevel)
        //yuan if (currentLevel == 0 || currentLevel <= closeLevel) {
		if (currentLevel == 0 || currentLevel == 100) {
            //yuan sendEvent(name: "windowShade", value: currentLevel == closeLevel ? "closed" : "open")
			sendEvent(name: "windowShade", value: currentLevel == 0 ? "open" : "closed")
        } else {
            if (lastLevel < currentLevel) {
                sendEvent([name:"windowShade", value: "closing"])
            } else if (lastLevel > currentLevel) {
                sendEvent([name:"windowShade", value: "opening"])
            }
        }
    }
    if (lastLevel != currentLevel) {
        log.debug "newlevel: '${newLevel}' currentlevel: '${currentLevel}' lastlevel: '${lastLevel}'."
        runIn(1, refresh)
    }
}

// Modify the reported level to compensate for Homekit assuming values other than 0 or 100 mean the shades are still opening/closing
def setReportedLevel(rawLevel) {
   sendEvent(name: "deviceLevel", value: rawLevel)
   if(rawLevel == closeLevel) {
       sendEvent(name: "level", value: 100)
       //yuan sendEvent(name: "position", value: 100)
   } else if (rawLevel ==  openLevel) {
       sendEvent(name: "level", value: 0)
       //yuan sendEvent(name: "position", value: 0)       
   } else {
       sendEvent(name: "level", value: rawLevel)
       //yuan sendEvent(name: "position", value: rawLevel)              
   }
}

def updateFinalState() {
    def level = device.currentValue("deviceLevel")
    log.debug "Running updateFinalState: '${level}'."
    sendEvent(name: "windowShade", value: level == closeLevel ? "closed" : "open")
}
                 
// Open Blinds Command
def open() {
    log.info "Fully Opening the Blinds."
	zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_OPEN)
}

def close() {
    log.info "Fully Closing the Blinds."
	zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_CLOSE)
}

// Set Position Command
def setPosition(value) {
    log.info "Setting the Blinds to level '${value}'."
	setShadeLevel(preset ?: value)
}

def setShadeLevel(value) {
	log.info "setShadeLevel($value)"

	Integer level = Math.max(Math.min(value as Integer, 100), 0)
	def cmd
	cmd = zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_GOTO_LIFT_PERCENTAGE, zigbee.convertToHexString(level, 2))

	return cmd
}

// Set Level Command
def setLevel(value, rate = null) {
    log.info "Setting the Blinds to level '${value}'."
	setShadeLevel(value)
}


// Return Level adjusted based on the Min/Max settings
def restrictLevelValue(value) {
    return value
}

// Pause the blinds
def pause() {
    log.info "Pausing the Blinds."
    zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_PAUSE)
}

// Stop Postition Change
def stopPositionChange() {
    pause()
}

// Start Postition Change
def startPositionChange(direction) {
    if(direction == "open") {
        open()
    } else if (direction == "close") {
        close()
    }
}

// Refresh the current state of the blinds
def refresh() {
    log.debug "Running refresh()"
    return zigbee.readAttribute(CLUSTER_WINDOW_COVERING, ATTRIBUTE_POSITION_LIFT) + zigbee.readAttribute(CLUSTER_BATTERY_LEVEL, BATTERY_PERCENTAGE_REMAINING)
}

// Configure Device Reporting and Bindings
def configure() {
    log.info "Configuring Device Reporting and Bindings."
    sendEvent(name: "checkInterval", value: 7320, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
    def cmds = zigbee.configureReporting(CLUSTER_WINDOW_COVERING, ATTRIBUTE_POSITION_LIFT, DataType.UINT8, 0, 600, 0x01) + zigbee.configureReporting(CLUSTER_BATTERY_LEVEL, 0x0021, DataType.UINT8, 600, 21600, 0x01)
	return refresh() + cmds
}

// Driver Update Event
def updated() {
    log.debug "Running updated()"
    unschedule()
}
