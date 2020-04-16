/*
	Generic Component Dimmer
	Copyright 2016 -> 2020 Hubitat Inc. All Rights Reserved
	2020-04-16 2.2.0 maxwell
		-add missing log method
	2019-09-07 2.1.5 maxwell
        -refactor declarations
	2018-12-15 maxwell
	    -initial pub

*/
metadata {
    definition(name: "Generic Component Dimmer", namespace: "hubitat", author: "mike maxwell", component: true) {
        capability "Light"
        capability "Switch"
        capability "Switch Level"
        capability "ChangeLevel"
        capability "Refresh"
        capability "Actuator"
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

void updated() {
    log.info "Updated..."
    log.warn "description logging is: ${txtEnable == true}"
}

void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    refresh()
}

void parse(String description) { log.warn "parse(String description) not implemented" }

void parse(List<Map> description) {
    description.each {
        if (it.name in ["switch","level"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
        }
    }
}

void on() {
    parent?.componentOn(this.device)
}

void off() {
    parent?.componentOff(this.device)
}

void setLevel(level) {
    parent?.componentSetLevel(this.device,level)
}

void setLevel(level, ramp) {
    parent?.componentSetLevel(this.device,level,ramp)
}

void startLevelChange(direction) {
    parent?.componentStartLevelChange(this.device,direction)
}

void stopLevelChange() {
    parent?.componentStopLevelChange(this.device)
}

void refresh() {
    parent?.componentRefresh(this.device)
}
