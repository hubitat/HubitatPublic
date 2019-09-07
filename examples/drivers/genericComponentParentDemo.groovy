/*
    Generic Component Parent Demo
    Copyright 2019 Hubitat Inc.  All Rights Reserved
    2019-09-07 (public repo only) maxwell
        -initial pub

*/

metadata {
    definition (name: "Generic Component Parent Demo", namespace: "hubitat", author: "Mike Maxwell") {
        capability "Configuration"
        
        //demo commands, these will create the appropriate component device if it doesn't already exist...
        command "childSwitchOn"
        command "childSwitchOff"
        command "childDimmerOn"
        command "childDimmerOff"
        command "childDimmerSetLevel", ["number"]
        command "setTemperature", ["number"]
        
    }
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

void logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void updated(){
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

void parse(String description) {
    //your parser here...
}

//demo custom commands
void childSwitchOn(){
    def cd = fetchChild("Switch")
    cd.parse([[name:"switch", value:"on", descriptionText:"${cd.displayName} was turned on"]])
}

void childSwitchOff(){
    def cd = fetchChild("Switch")
    cd.parse([[name:"switch", value:"off", descriptionText:"${cd.displayName} was turned off"]])
}

void childDimmerOn(){
    def cd = fetchChild("Dimmer")
    List<Map> evts = []
    evts.add([name:"switch", value:"on", descriptionText:"${cd.displayName} was turned on"])
    Integer cv = cd.currentValue("level").toInteger()
    evts.add([name:"level", value:cv, descriptionText:"${cd.displayName} level was set to ${cv}%", unit: "%"])
    cd.parse(evts)
}

void childDimmerOff(){
    def cd = fetchChild("Dimmer")
    cd.parse([[name:"switch", value:"off", descriptionText:"${cd.displayName} was turned off"]])
}

void childDimmerSetLevel(level){
    def cd = fetchChild("Dimmer")
    List<Map> evts = []
    String cv = cd.currentValue("switch")
    if (cv == "off") evts.add([name:"switch", value:"on", descriptionText:"${cd.displayName} was turned on"])
    evts.add([name:"level", value:level, descriptionText:"${cd.displayName} level was set to ${level}%", unit: "%"])
    cd.parse(evts)    
}

void setTemperature(value){
    def cd = fetchChild("Temperature Sensor")
    String unit = "°${location.temperatureScale}"
    cd.parse([[name:"temperature", value:value, descriptionText:"${cd.displayName} temperature is ${value}${unit}", unit: unit]])
}

def fetchChild(String type){
    String thisId = device.id
    def cd = getChildDevice("${thisId}-${type}")
    if (!cd) {
        cd = addChildDevice("hubitat", "Generic Component ${type}", "${thisId}-${type}", [name: "${device.displayName} ${type}", isComponent: true])
        //set initial attribute values, with a real device you would not do this here...
        List<Map> defaultValues = []
        switch (type) {
            case "Switch":
                defaultValues.add([name:"switch", value:"off", descriptionText:"set initial switch value"])
                break
            case "Dimmer":
                defaultValues.add([name:"switch", value:"off", descriptionText:"set initial switch value"])
                defaultValues.add([name:"level", value:50, descriptionText:"set initial level value", unit:"%"])
                break
            case "Temperature Sensor" :
                String unit = "°${location.temperatureScale}"
                BigInteger value = (unit == "°F") ? 70.0 : 21.0
                defaultValues.add([name:"temperature", value:value, descriptionText:"set initial temperature value", unit:unit])
                break
            default :
                log.warn "unable to set initial values for type:${type}"
                break
        }
        cd.parse(defaultValues)
    }
    return cd 
}

//child device methods
void componentRefresh(cd){
    if (logEnable) log.info "received refresh request from ${cd.displayName}"
}

void componentOn(cd){
    if (logEnable) log.info "received on request from ${cd.displayName}"
    getChildDevice(cd.deviceNetworkId).parse([[name:"switch", value:"on", descriptionText:"${cd.displayName} was turned on"]])
}

void componentOff(cd){
    if (logEnable) log.info "received off request from ${cd.displayName}"
    getChildDevice(cd.deviceNetworkId).parse([[name:"switch", value:"off", descriptionText:"${cd.displayName} was turned off"]])
}

void componentSetLevel(cd,level,transitionTime = null) {
    if (logEnable) log.info "received setLevel(${level}, ${transitionTime}) request from ${cd.displayName}"
    getChildDevice(cd.deviceNetworkId).parse([[name:"level", value:level, descriptionText:"${cd.displayName} level was set to ${level}%", unit: "%"]])
}

void componentStartLevelChange(cd, direction) {
    if (logEnable) log.info "received startLevelChange(${direction}) request from ${cd.displayName}"
}

void componentStopLevelChange(cd) {
    if (logEnable) log.info "received stopLevelChange request from ${cd.displayName}"
}


List<String> configure() {
    log.warn "configure..."
    runIn(1800,logsOff)
    //your configuration commands here...
}

