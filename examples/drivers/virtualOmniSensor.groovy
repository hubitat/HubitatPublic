// Copyright 2016-2019 Hubitat Inc.  All Rights Reserved

metadata {
    definition (name: "Virtual Omni Sensor", namespace: "hubitat", author: "Bruce Ravenel") {
        capability "Presence Sensor"
        capability "Acceleration Sensor"
        capability "Carbon Dioxide Measurement"
        capability "Carbon Monoxide Detector"
        capability "Contact Sensor"
        capability "Illuminance Measurement"
        capability "Motion Sensor"
        capability "Relative Humidity Measurement"
        capability "Smoke Detector"
        capability "Temperature Measurement"
        capability "Water Sensor"
        capability "Energy Meter"
        capability "Power Meter"
        command "arrived"
        command "departed"
        command "accelerationActive"
        command "accelerationInactive"
        command "motionActive"
        command "motionInactive"
        command "open"
        command "close"
        command "CODetected"
        command "COClear"
        command "smokeDetected"
        command "smokeClear"
        command "setCarbonDioxide", ["Number"]
        command "setIlluminance", ["Number"]
        command "setRelativeHumidity", ["Number"]
        command "setTemperature", ["Number"]
        command "wet"
        command "dry"
        command "setVariable", ["String"]
        command "setEnergy", ["Number"]
        command "setPower", ["Number"]
        attribute "variable", "String"
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

def installed() {
    log.warn "installed..."
    arrived()
    accelerationInactive()
    COClear()
    close()
    setIlluminance(50)
    setCarbonDioxide(350)
    setRelativeHumidity(35)
    motionInactive()
    smokeClear()
    setTemperature(70)
    dry()
    runIn(1800,logsOff)
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

def parse(String description) {
}

def arrived() {
    def descriptionText = "${device.displayName} has arrived"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "presence", value: "present",descriptionText: descriptionText)
}

def departed() {
    def descriptionText = "${device.displayName} has departed"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "presence", value: "not present",descriptionText: descriptionText)
}

def accelerationActive() {
    def descriptionText = "${device.displayName} is active"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "acceleration", value: "active", descriptionText: descriptionText)
}

def accelerationInactive() {
    def descriptionText = "${device.displayName} is inactive"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "acceleration", value: "inactive", descriptionText: descriptionText)
}

def CODetected() {
    def descriptionText = "${device.displayName} CO detected"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "carbonMonoxide", value: "detected", descriptionText: descriptionText)
}

def COClear() {
    def descriptionText = "${device.displayName} CO clear"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "carbonMonoxide", value: "clear", descriptionText: descriptionText)
}

def open() {
    def descriptionText = "${device.displayName} is open"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "contact", value: "open", descriptionText: descriptionText)
}

def close() {
    def descriptionText = "${device.displayName} is closed"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "contact", value: "closed", descriptionText: descriptionText)
}

def setIlluminance(lux) {
    def descriptionText = "${device.displayName} is ${lux} lux"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "illuminance", value: lux, descriptionText: descriptionText, unit: "Lux")
}

def setCarbonDioxide(CO2) {
    def descriptionText = "${device.displayName}  Carbon Dioxide is ${CO2} ppm"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "carbonDioxide", value: CO2, descriptionText: descriptionText, unit: "ppm")
}

def setRelativeHumidity(humid) {
    def descriptionText = "${device.displayName} is ${humid}% humidity"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "humidity", value: humid, descriptionText: descriptionText, unit: "RH%")
}

def smokeDetected() {
    def descriptionText = "${device.displayName} smoke detected"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "smoke", value: "detected", descriptionText: descriptionText)
}

def motionActive() {
    def descriptionText = "${device.displayName} is active"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "motion", value: "active", descriptionText: descriptionText)
}

def motionInactive() {
    def descriptionText = "${device.displayName} is inactive"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "motion", value: "inactive", descriptionText: descriptionText)
}

def smokeClear() {
    def descriptionText = "${device.displayName} smoke clear"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "smoke", value: "clear", descriptionText: descriptionText)
}

def setTemperature(temp) {
    def unit = "°${location.temperatureScale}"
    def descriptionText = "${device.displayName} is ${temp}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "temperature", value: temp, descriptionText: descriptionText, unit: unit)
}

def wet() {
    def descriptionText = "${device.displayName} water wet"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "water", value: "wet", descriptionText: descriptionText)
}

def dry() {
    def descriptionText = "${device.displayName} water dry"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "water", value: "dry", descriptionText: descriptionText)
}

def setVariable(str) {
    def descriptionText = "${device.displayName} variable is ${str}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "variable", value: str, descriptionText: descriptionText)
}

def setEnergy(energy) {
    def descriptionText = "${device.displayName} is ${energy} energy"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "energy", value: energy, descriptionText: descriptionText)
}

def setPower(power) {
    def descriptionText = "${device.displayName} is ${power} power"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "power", value: power, descriptionText: descriptionText)
}
