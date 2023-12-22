/*************
*  Virtual Omni Sensor Plus
*  Copyright 2021 Terrel Allen All Rights Reserved
*
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU Affero General Public License as published
*  by the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Affero General Public License for more details.
*
*  You should have received a copy of the GNU Affero General Public License
*  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*  WARNING!!!
*  Use at your own risk.
*  Modify at your own risk.
*
*  USAGE
*  Repalces existing Virtual Omni Sensor driver
*  Adds additonal capabilities, commands, and attributes not available
*      in the default Virtual Omni Sensor driver
*
*  CHANGE LOG
*  v202110031411
*      -add threeAxis
*  v202110022249
*      -updates to event descriptive text for humidity
*      -add hours and minutes to change log dates
*  v202110020000
*      -change batteryLevel attribute back to battery
*  v202110010000
*      -add header
*      -add battery status
*      -add battery last updated
*  v20210930
*      -initial release w/ battery level and tamper
*
*************/

metadata {
    definition (name: "Virtual Omni Sensor Plus",
                namespace: "whodunitGorilla",
                author: "Terrel Allen",
                importUrl: "https://raw.githubusercontent.com/terrelsa13/HubitatPublic/master/examples/drivers/virtualOmniSensorPlus.groovy")
    {
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
        capability "Battery"
        capability "Tamper Alert"
        capability "Three Axis"
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
        command "setBattery", ["Number"]
        command "batteryStatusIdle"
        command "batteryStatusDischarging"
        command "batteryStatusCharging"
        command "tamperClear"
        command "tamperDetected"
        command "threeAxis", [[name:"x",type:"NUMBER", description:"X-Axis", constraints:["NUMBER"]],[name:"y",type:"NUMBER", description:"Y-Axis", constraints:["NUMBER"]],[name:"z",type:"NUMBER", description:"Z-Axis", constraints:["NUMBER"]]]
        command "setThreeAxis"
        attribute "variable", "String"
        attribute "batteryStatus", "String"
        attribute "batteryLastUpdated", "Date"
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
    initialized()
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
    setBatteryStatus("unknown")
    tamperClear()
    runIn(1800,logsOff)
}

void initialized() {
    //Clear warning count for out of range battery level
    state.warningCount = 0
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
    def descriptionText = "${device.displayName} acceleration is active"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "acceleration", value: "active", descriptionText: descriptionText)
}

def accelerationInactive() {
    def descriptionText = "${device.displayName} acceleration is inactive"
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
    def descriptionText = "${device.displayName} humidity is ${humid}%"
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
    def descriptionText = "${device.displayName} temperature is ${temp}${unit}"
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

String getBatteryStatus() {
    //return battery status
    return device.currentValue("batteryStatus")
}

def setBatteryStatus(status) {
    def descriptionText = "${device.displayName} battery status is ${status}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "batteryStatus", value: status, descriptionText: descriptionText)
}

def batteryStatusIdle() {
    setBatteryStatus("idle")
}

def batteryStatusDischarging() {
    setBatteryStatus("discharging")
}

def batteryStatusCharging() {
    setBatteryStatus("charging")
}

Number getBattery() {
    //return battery level
    return device.currentValue('battery')
}

//Command to set the battery level
def setBattery(level) {
//def battery(level) {
    //Check battery level is 0 - 100
    if(level >= 0 && level <= 100) {
        //Get current date and time
        Date lastUpdate = new Date()
        //Get previous battery level
        Number prevBatteryLevel = getBattery()
        //No battery level set
        if (prevBatteryLevel == null) {
            setBatteryStatus("unknown")
        }
        //No change in battery level
        else if (prevBatteryLevel == level) {
            setBatteryStatus("idle")
        }
        //Battery level decreasing
        else if (prevBatteryLevel > level) {
            setBatteryStatus("discharging")
        }
        //Battery level increasing
        else /*(prevBatteryLevel < level)*/ {
            setBatteryStatus("charging")
        }
        //Set unit
        unit = "%"
        def descriptionTextLevel = "${device.displayName} battery level is ${level}${unit}"
        def descriptionTextLastUpdate = "${device.displayName} battery information last updated ${lastUpdate}"
        if (txtEnable) log.info "${descriptionTextLevel}"
        if (txtEnable) log.info "${descriptionTextLastUpdate}"
        //Update attributes
        sendEvent(name: "battery", value: level, descriptionText: descriptionTextLevel, unit: unit)
        sendEvent(name: "batteryLastUpdated", value : lastUpdate.format("yyyy/MM/dd HH:mm:ss"), descriptionText: descriptionTextLastUpdate)
        //Reset warning count if there have been previous warnings
        if (state.warningCount > 0) {
            state.warningCount = 0
            log.info("setBattery(): Warning count reset")
        } 
    }
    // If the battery reading is outside the 0 - 100 range, log a warning and leave the current reading in place
    //   use the warning count state variable to make sure we don't spam the logs with repeated warnings
    else {
        if (state.warningCount < 10) {
            state.warningCount++
            if (getBattery() == null) {
                log.warn("setBattery(): Warning (#${state.warningCount}) - Battery level outside of 0%-100% range, ${device.displayName} not updated. Retaining ${prevBatteryLevel} battery level")
            }
            else {
                log.warn("setBattery(): Warning (#${state.warningCount}) - Battery level outside of 0%-100% range, ${device.displayName} not updated. Retaining prevoius battery level ${prevBatteryLevel}%")
            }
        }
    }
}

//The other command to set battery level
def battery(level) {
    setBattery(level)
}

def tamperClear() {
    def descriptionText = "${device.displayName} tamper is clear"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "tamper", value: "clear", descriptionText: descriptionText)
}

def tamperDetected() {
    def descriptionText = "${device.displayName} tamper is detected"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "tamper", value: "detected", descriptionText: descriptionText)
}

//threeAxis(integer,integer,integer) input format: 0,0,0
def threeAxis(x,y,z) {
    def xyz = "x:${x},y:${y},z:${z}"
    def descriptionText = "${device.displayName} threeAxis is ${xyz}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "threeAxis", value: xyz, descriptionText: descriptionText)
    sendEvent(name: "xAxis", value: x)
    sendEvent(name: "yAxis", value: y)
    sendEvent(name: "zAxis", value: z)
}

//setThreeAxis(string) input format: [x:0,y:0,z:0]
def setThreeAxis(xyz) {
    if (xyz != null) {
        //remove open bracket
        removeBrackets = xyz.minus("[")
        //remove close bracket
        removeBrackets = removeBrackets.minus("]")
        //split string into an array at ","
        threeAxisArray = removeBrackets.split(",")
        //split strings into arrys at ":"
        xPair = threeAxisArray[0].split(":")
        yPair = threeAxisArray[1].split(":")
        zPair = threeAxisArray[2].split(":")
        //to integers
        int x = xPair[1] as Integer
        int y = yPair[1] as Integer
        int z = zPair[1] as Integer
        //command
        threeAxis(x,y,z)
    }
}