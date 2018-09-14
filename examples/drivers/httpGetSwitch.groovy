/*
 * Http GET Switch
 *
 * Calls URIs with HTTP GET for switch on or off
 * 
 */
metadata {
    definition (name: "Http GET Switch", namespace: "community", author: "Community") {
        capability "Actuator"
        capability "Switch"
        capability "Sensor"
    }
}

preferences {
    section("URIs"){
        input "onURI", "text", title: "On URI", required: false
        input "offURI", "text", title: "Off URI", required: false
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def updated(){
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

def parse(String description) {
    if (logEnable) log.debug(description)
}

def on() {
    if (logEnable) log.debug "Sending on GET request to [${settings.onURI}]"

    httpGet(settings.onURI) {resp ->
        if (resp.data) {
            if (logEnable) log.debug "${resp.data}"
        } 
    }
    sendEvent(name: "switch", value: "on", isStateChange: true) 
}

def off() {
    if (logEnable) log.debug "Sending off GET request to [${settings.offURI}]"
    httpGet(settings.offURI) {resp ->
        if (resp.data) {
            if (logEnable) log.debug "${resp.data}"
        } 
    }
    sendEvent(name: "switch", value: "off", isStateChange: true)
}
