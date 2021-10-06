/*************
*  Virtual Zooz ZSE11
*  Virtual Zooz ZSE40
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
*  n/a
*
*  CHANGE LOG
*  v202110031855
*      -initial release
*
*************/

metadata {
    definition (name: "Virtual Fibaro ZW5",
                namespace: "whodunitGorilla",
                author: "Terrel Allen",
                importUrl: "https://raw.githubusercontent.com/terrelsa13/HubitatPublic/master/examples/drivers/virtualFibaroZW5.groovy")
    {
        capability "Illuminance Measurement"
        capability "Motion Sensor"
        capability "Relative Humidity Measurement"
        capability "Temperature Measurement"
        capability "Battery"
        capability "Tamper Alert"
        command "motionActive"
        command "motionInactive"
        command "setIlluminance", ["Number"]
        command "setRelativeHumidity", ["Number"]
        command "setTemperature", ["Number"]
        command "setVariable", ["String"]
        command "setBattery", ["Number"]
        command "batteryStatusIdle"
        command "batteryStatusDischarging"
        command "batteryStatusCharging"
        command "tamperClear"
        command "tamperDetected"
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
    setIlluminance(50)
    setRelativeHumidity(35)
    motionInactive()
    setTemperature(70)
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

def setIlluminance(lux) {
    def descriptionText = "${device.displayName} is ${lux} lux"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "illuminance", value: lux, descriptionText: descriptionText, unit: "Lux")
}

def setRelativeHumidity(humid) {
    def descriptionText = "${device.displayName} humidity is ${humid}%"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "humidity", value: humid, descriptionText: descriptionText, unit: "RH%")
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

def setTemperature(temp) {
    def unit = "°${location.temperatureScale}"
    def descriptionText = "${device.displayName} temperature is ${temp}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "temperature", value: temp, descriptionText: descriptionText, unit: unit)
}

def setVariable(str) {
    def descriptionText = "${device.displayName} variable is ${str}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "variable", value: str, descriptionText: descriptionText)
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