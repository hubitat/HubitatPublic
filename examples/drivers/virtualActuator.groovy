/*
 	Virtual Actuator

    Copyright 2016-2021 Hubitat Inc. All Rights Reserved


*/

metadata {
	definition (name: "Virtual Actuator", namespace: "hubitat", author: "Bruce Ravenel") {
		capability	"Actuator"
		command 	"on"
		command 	"off"
		command		"neutral"
        attribute	"switchPosition", "ENUM"
	}
	preferences {
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
}

def installed() {
	log.warn "installed..."
	off()
	select("off")
}

def updated() {
	log.warn "updated..."
	log.warn "description logging is: ${txtEnable == true}"
}

def parse(String description) {
}

def on() {
	def descriptionText = "${device.displayName} was turned on"
	if (txtEnable) log.info "${descriptionText}"
	sendEvent(name: "switchPosition", value: "on", descriptionText: descriptionText)
}

def off() {
	def descriptionText = "${device.displayName} was turned off"
	if (txtEnable) log.info "${descriptionText}"
	sendEvent(name: "switchPosition", value: "off", descriptionText: descriptionText)
}

def neutral() {
	def descriptionText = "${device.displayName} was turned neutral"
	if (txtEnable) log.info "${descriptionText}"
	sendEvent(name: "switchPosition", value: "neutral", descriptionText: descriptionText)
}
