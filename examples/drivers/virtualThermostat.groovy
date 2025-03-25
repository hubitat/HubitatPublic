/*
	Virtual Thermostat

	Copyright 2016 -> 2023 Hubitat Inc.  All Rights Reserved

*/

metadata {
	definition (
			name: "Virtual Thermostat",
			namespace: "hubitat",
			author: "Kevin L., Mike M., Bruce R."
	) {
		capability "Actuator"
		capability "Sensor"
		capability "Temperature Measurement"
		capability "Thermostat"

		attribute "supportedThermostatFanModes", "JSON_OBJECT"
		attribute "supportedThermostatModes", "JSON_OBJECT"
		attribute "hysteresis", "NUMBER"

		// Commands needed to change internal attributes of virtual device.
		command "setTemperature", ["NUMBER"]
		command "setThermostatOperatingState", ["ENUM"]
		command "setThermostatSetpoint", ["NUMBER"]
		command "setSupportedThermostatFanModes", ["JSON_OBJECT"]
		command "setSupportedThermostatModes", ["JSON_OBJECT"]
	}

	preferences {
		input( name: "hysteresis",type:"enum",title: "Thermostat hysteresis degrees", options:["0.1","0.25","0.5","1","2"], description:"", defaultValue: 0.5)
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
	}
}
import groovy.json.JsonOutput

def installed() {
	log.warn "installed..."
	initialize()
}

def updated() {
	log.info "updated..."
	log.warn "debug logging is: ${logEnable == true}"
	log.warn "description logging is: ${txtEnable == true}"
	if (logEnable) runIn(1800,logsOff)
	initialize()
}

def initialize() {
	if (state?.lastRunningMode == null) {
		sendEvent(name: "temperature", value: convertTemperatureIfNeeded(68.0,"F",1))
		sendEvent(name: "thermostatSetpoint", value: convertTemperatureIfNeeded(68.0,"F",1))
		sendEvent(name: "heatingSetpoint", value: convertTemperatureIfNeeded(68.0,"F",1))
		sendEvent(name: "coolingSetpoint", value: convertTemperatureIfNeeded(75.0,"F",1))
		state.lastRunningMode = "heat"
		updateDataValue("lastRunningMode", "heat")
		setThermostatOperatingState("idle")
		setSupportedThermostatFanModes(JsonOutput.toJson(["auto","circulate","on"]))
		setSupportedThermostatModes(JsonOutput.toJson(["auto", "cool", "emergency heat", "heat", "off"]))
		off()
		fanAuto()
	}
	sendEvent(name: "hysteresis", value: (hysteresis ?: 0.5).toBigDecimal())
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def manageCycle(){
	def ambientTempChangePerCycle = 0.25
	def hvacTempChangePerCycle = 0.75

	def hysteresis = (hysteresis ?: 0.5).toBigDecimal()

	def coolingSetpoint = (device.currentValue("coolingSetpoint") ?: convertTemperatureIfNeeded(75.0,"F",1)).toBigDecimal()
	def heatingSetpoint = (device.currentValue("heatingSetpoint") ?: convertTemperatureIfNeeded(68.0,"F",1)).toBigDecimal()
	def temperature = (device.currentValue("temperature") ?: convertTemperatureIfNeeded(68.0,"F",1)).toBigDecimal()

	def thermostatMode = device.currentValue("thermostatMode") ?: "off"
	def thermostatOperatingState = device.currentValue("thermostatOperatingState") ?: "idle"

	def ambientGain = (temperature + ambientTempChangePerCycle).setScale(2)
	def ambientLoss = (temperature - ambientTempChangePerCycle).setScale(2)
	def coolLoss = (temperature - hvacTempChangePerCycle).setScale(2)
	def heatGain = (temperature + hvacTempChangePerCycle).setScale(2)

	def coolingOn = (temperature >= (coolingSetpoint + hysteresis))
	if (thermostatOperatingState == "cooling") coolingOn = temperature >= (coolingSetpoint - hysteresis)

	def heatingOn = (temperature <= (heatingSetpoint - hysteresis))
	if (thermostatOperatingState == "heating") heatingOn = (temperature <= (heatingSetpoint + hysteresis))
	
	if (thermostatMode == "cool") {
		if (coolingOn && thermostatOperatingState != "cooling") setThermostatOperatingState("cooling")
		else if (thermostatOperatingState != "idle") setThermostatOperatingState("idle")
	} else if (thermostatMode == "heat") {
		if (heatingOn && thermostatOperatingState != "heating") setThermostatOperatingState("heating")
		else if (thermostatOperatingState != "idle") setThermostatOperatingState("idle")
	} else if (thermostatMode == "auto") {
		if (heatingOn && coolingOn) log.error "cooling and heating are on- temp:${temperature}"
		else if (coolingOn && thermostatOperatingState != "cooling") setThermostatOperatingState("cooling")
		else if (heatingOn && thermostatOperatingState != "heating") setThermostatOperatingState("heating")
		else if ((!coolingOn || !heatingOn) && thermostatOperatingState != "idle") setThermostatOperatingState("idle")
	}
}

// Commands needed to change internal attributes of virtual device.
def setTemperature(temperature) {
	logDebug "setTemperature(${temperature}) was called"
	sendTemperatureEvent("temperature", temperature)
	runIn(1, manageCycle)
}

def setHumidity(humidity) {
	logDebug "setHumidity(${humidity}) was called"
	sendEvent(name: "humidity", value: humidity, unit: "%", descriptionText: getDescriptionText("humidity set to ${humidity}%"))
}

def setThermostatOperatingState (operatingState) {
	logDebug "setThermostatOperatingState (${operatingState}) was called"
	updateSetpoints(null,null,null,operatingState)
	sendEvent(name: "thermostatOperatingState", value: operatingState, descriptionText: getDescriptionText("thermostatOperatingState set to ${operatingState}"))
}

def setSupportedThermostatFanModes(fanModes) {
	logDebug "setSupportedThermostatFanModes(${fanModes}) was called"
	// (auto, circulate, on)
	sendEvent(name: "supportedThermostatFanModes", value: fanModes, descriptionText: getDescriptionText("supportedThermostatFanModes set to ${fanModes}"))
}

def setSupportedThermostatModes(modes) {
	logDebug "setSupportedThermostatModes(${modes}) was called"
	// (auto, cool, emergency heat, heat, off)
	sendEvent(name: "supportedThermostatModes", value: modes, descriptionText: getDescriptionText("supportedThermostatModes set to ${modes}"))
}


def auto() { setThermostatMode("auto") }

def cool() { setThermostatMode("cool") }

def emergencyHeat() { setThermostatMode("heat") }

def heat() { setThermostatMode("heat") }
def off() { setThermostatMode("off") }

def setThermostatMode(mode) {
	sendEvent(name: "thermostatMode", value: "${mode}", descriptionText: getDescriptionText("thermostatMode is ${mode}"))
	setThermostatOperatingState ("idle")
	updateSetpoints(null, null, null, mode)
	runIn(1, manageCycle)
}

def fanAuto() { setThermostatFanMode("auto") }
def fanCirculate() { setThermostatFanMode("circulate") }
def fanOn() { setThermostatFanMode("on") }

def setThermostatFanMode(fanMode) {
	sendEvent(name: "thermostatFanMode", value: "${fanMode}", descriptionText: getDescriptionText("thermostatFanMode is ${fanMode}"))
}

def setThermostatSetpoint(setpoint) {
	logDebug "setThermostatSetpoint(${setpoint}) was called"
	updateSetpoints(setpoint, null, null, null)
}

def setCoolingSetpoint(setpoint) {
	logDebug "setCoolingSetpoint(${setpoint}) was called"
	updateSetpoints(null, null, setpoint, null)
}

def setHeatingSetpoint(setpoint) {
	logDebug "setHeatingSetpoint(${setpoint}) was called"
	updateSetpoints(null, setpoint, null, null)
}

private updateSetpoints(sp = null, hsp = null, csp = null, operatingState = null){
	if (operatingState in ["off"]) return
	if (hsp == null) hsp = device.currentValue("heatingSetpoint",true)
	if (csp == null) csp = device.currentValue("coolingSetpoint",true)
	if (sp == null) sp = device.currentValue("thermostatSetpoint",true)

	if (operatingState == null) operatingState = state.lastRunningMode

	def hspChange = isStateChange(device,"heatingSetpoint",hsp.toString())
	def cspChange = isStateChange(device,"coolingSetpoint",csp.toString())
	def spChange = isStateChange(device,"thermostatSetpoint",sp.toString())
	def osChange = operatingState != state.lastRunningMode

	def newOS
	def descriptionText
	def name
	def value
	def unit = "°${location.temperatureScale}"
	switch (operatingState) {
		case ["pending heat","heating","heat"]:
			newOS = "heat"
			if (spChange) {
				hspChange = true
				hsp = sp
			} else if (hspChange || osChange) {
				spChange = true
				sp = hsp
			}
			if (csp - 2 < hsp) {
				csp = hsp + 2
				cspChange = true
			}
			break
		case ["pending cool","cooling","cool"]:
			newOS = "cool"
			if (spChange) {
				cspChange = true
				csp = sp
			} else if (cspChange || osChange) {
				spChange = true
				sp = csp
			}
			if (hsp + 2 > csp) {
				hsp = csp - 2
				hspChange = true
			}
			break
		default :
			return
	}

	if (hspChange) {
		value = hsp
		name = "heatingSetpoint"
		descriptionText = "${device.displayName} ${name} was set to ${value}${unit}"
		if (txtEnable) log.info descriptionText
		sendEvent(name: name, value: value, descriptionText: descriptionText, unit: unit, isStateChange: true)
	}
	if (cspChange) {
		value = csp
		name = "coolingSetpoint"
		descriptionText = "${device.displayName} ${name} was set to ${value}${unit}"
		if (txtEnable) log.info descriptionText
		sendEvent(name: name, value: value, descriptionText: descriptionText, unit: unit, isStateChange: true)
	}
	if (spChange) {
		value = sp
		name = "thermostatSetpoint"
		descriptionText = "${device.displayName} ${name} was set to ${value}${unit}"
		if (txtEnable) log.info descriptionText
		sendEvent(name: name, value: value, descriptionText: descriptionText, unit: unit, isStateChange: true)
	}

	state.lastRunningMode = newOS
	updateDataValue("lastRunningMode", newOS)
}

def setSchedule(schedule) {
	sendEvent(name: "schedule", value: "${schedule}", descriptionText: getDescriptionText("schedule is ${schedule}"))
}

private sendTemperatureEvent(name, val) {
	sendEvent(name: "${name}", value: val, unit: "°${getTemperatureScale()}", descriptionText: getDescriptionText("${name} is ${val} °${getTemperatureScale()}"), isStateChange: true)
}


def parse(String description) {
	logDebug "$description"
}


private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}
