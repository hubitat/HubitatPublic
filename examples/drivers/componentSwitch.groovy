/*
	Generic Component Switch
	Copyright 2016, 2017, 2018 Hubitat Inc. All Rights Reserved

	2018-12-15 maxwell
	    -initial pub

*/

metadata {
    definition(name: "Generic Component Switch", namespace: "hubitat", author: "mike maxwell", component: true) {
        capability "Switch"
        capability "Refresh"
        capability "Actuator"
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def updated() {
    log.info "Updated..."
    log.warn "description logging is: ${txtEnable == true}"
}

def installed() {
    "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    refresh()
}

def parse(String description) { log.warn "parse(String description) not implemented" }

def parse(List description) {
    description.each {
        if (it.name in ["switch"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
        }
    }
    return
}

def on() {
    parent?.componentOn(this.device)
}

def off() {
    parent?.componentOff(this.device)
}

def refresh() {
    parent?.componentRefresh(this.device)
}
