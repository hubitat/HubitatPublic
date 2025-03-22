/*
    Sofabaton X1S
	Copyright 2025 Hubitat Inc. All Rights Reserved

	2025-03-22 maxwell
		-initial publication in github repo

	*simple example driver for Sofabaton X1S remote, allows mapping X1S remote buttons to Hubitat button events

	*driver configuration
	-set a static DHCP reservation for the XS1 hub
	-use that reserved IP in this drivers preference setting

	
	*mobile app configuration on the X1S side for this specific driver instance:
	-click add devices in devices tab, select Wi-Fi
	-click link at bottom "Create a virtual device for IP control"
	-enter http://my hubs IP address:39501/route (the route isn't actually used in this example, so the name could be anything, but it is required else the command won't be sent)
	-set PUT as the request method, application/json as the content type
	-in the body type the command text (no quotes), that you want to parse, toggle in this example code

*/

metadata {
    definition (name: "Sofabaton X1S", namespace: "hubitat", author: "Mike Maxwell") {
        capability "Actuator"
        capability "PushableButton"
        preferences {
            input name:"ip", type:"text", title: "X1S IP"
            input name:"logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
            input name:"txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        }
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
    if (ip) {
        device.deviceNetworkId = ipToHex(IP)
        //change button count to suit your needs
        sendEvent(name:"numberOfButtons",value:10)
    }
}

void parse(String description) {
    Map msg = parseLanMessage(description)
    switch (msg.body) {
        //add other case commands below
        case "toggle" :
        	sendButtonEvent("pushed", 1, "physical")
        	break
        //case "yada" :
        	//sendButtonEvent("pushed", 2, "physical")
        	//break
        default :
        	log.debug "unknown body:${msg.body}"
    }
}

void sendButtonEvent(String evt, Integer bid, String type) {
    String descriptionText = "${device.displayName} button ${bid} was ${evt} [${type}]"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: evt, value: bid, descriptionText: descriptionText, isStateChange: true, type: type)
}

void push(button) {
    sendButtonEvent("pushed", button, "digital", "driver UI")
}

String ipToHex(IP) {
    List<String> quad = ip.split(/\./)
    String hexIP = ""
    quad.each {
        hexIP+= Integer.toHexString(it.toInteger()).padLeft(2,"0").toUpperCase()
    }
    return hexIP
}
