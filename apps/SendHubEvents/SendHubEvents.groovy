/*
 *  Copyright 2017, 2018 Hubitat, Inc.  All Rights Reserved.
 *
 *  This software if free for Private Use. You may use and modify the software without distributing it.
 *  You may not grant a sublicense to modify and distribute this software to third parties.
 *  Software is provided without warranty and your use of it is at your own risk.
 *
 */
definition(
    name: "Send Hub Events",
    namespace: "hubitat",
    author: "Charles Schwer, Mike Maxwell and Bruce Ravenel",
    description: "Send events to another hub",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	page(name: "main")
}

def main(){
	return (
    	dynamicPage(name: "main", title: "Send Hub Events", uninstall: true, install: true){
      		section("Monitor these devices...") {
            	input "presenceDevices", "capability.presenceSensor", title: "Presence Devices", multiple: true, required: false
            	input "motionDevices", "capability.motionSensor", title: "Motion Sensors (motion, temperature)", multiple: true, required: false
            	input "contactDevices", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: false
            	input "accelerationDevices", "capability.accelerationSensor", title: "Acceleration Sensors", multiple: true, required: false
                input "multiSensors", "capability.contactSensor", title: "Multi Sensors (contact, acceleration, temperature)", multiple: true, required: false
                input "omniSensors", "capability.sensor", title: "Omni Sensors (presence, contact, acceleration, temperature, carbonMonoxide, illuminance, motion, water, smoke)", multiple: true, required: false
            	input "switchDevices", "capability.switch", title: "Switches", multiple: true, required: false
                input "dimmerDevices", "capability.switchLevel", title: "Dimmers", multiple: true, required: false
                input "logEnable", "bool", title: "Enable debug logging", required: false
			}
    		section(" ") {
    			input "enabled", "bool", title: "Enable Hub Link?", required: false
    		}
    		section ("Device to push data to") {
    			input "ip", "text", title:"Hubitat Hub IP", required: true
        		input "port", "text", title:"Port", defaultValue: "39501", required: false
    		}
        }
    )
}

def installed() {
	initialize()
}


def updated() {
	unsubscribe()
	initialize()
}


def initialize() {
    subscribe(presenceDevices, "presence", handleDeviceEvent)
    subscribe(motionDevices, "motion", handleDeviceEvent)
    subscribe(motionDevices, "temperature", handleDeviceEvent)
    subscribe(contactDevices, "contact", handleDeviceEvent)
    subscribe(accelerationDevices, "acceleration", handleDeviceEvent)
    subscribe(multiSensors, "contact", handleDeviceEvent)
    subscribe(multiSensors, "acceleration", handleDeviceEvent)
    subscribe(multiSensors, "temperature", handleDeviceEvent)
    subscribe(omniSensors, "presence", omniDeviceEvent)
    subscribe(omniSensors, "contact", omniDeviceEvent)
    subscribe(omniSensors, "acceleration", omniDeviceEvent)
    subscribe(omniSensors, "temperature", omniDeviceEvent)
    subscribe(omniSensors, "carbonMonoxide", omniDeviceEvent)
    subscribe(omniSensors, "illuminance", omniDeviceEvent)
    subscribe(omniSensors, "motion", omniDeviceEvent)
    subscribe(omniSensors, "water", omniDeviceEvent)
    subscribe(omniSensors, "smoke", omniDeviceEvent)
    subscribe(switchDevices, "switch", handleDeviceEvent)
    subscribe(dimmerDevices, "switch", handleDeviceEvent)
    subscribe(dimmerDevices, "level", handleDeviceEvent)
    sendSetup()
}

def handleDeviceEvent(evt) {
def dni = "stHub_${evt?.device?.deviceNetworkId}"
def msg = """POST / HTTP/1.1
HOST: ${ip}:${port}
CONTENT-TYPE: text/plain
DEVICE-NETWORK-ID: ${dni}
CONTENT-LENGTH: ${evt.value.length()}

${evt.value}
"""
	if(enabled) {
		if (logEnable) log.debug "Name: ${evt.device.displayName}, DNI: ${dni}, value: ${evt.value}"
		sendHubCommand(new physicalgraph.device.HubAction(msg, physicalgraph.device.Protocol.LAN, "${ip}:${port}"))
    }
}

def omniDeviceEvent(evt) {
def dni = "stHub_${evt?.device?.deviceNetworkId}"
def msg = """POST / HTTP/1.1
HOST: ${ip}:${port}
CONTENT-TYPE: text/plain
DEVICE-NETWORK-ID: ${dni}
CONTENT-LENGTH: ${(evt.name.length() + evt.value.length() + 1)}

${evt.name}:${evt.value}
"""
	if(enabled) {
        if (logEnable) log.debug "Name: ${evt.device.displayName}, DNI: ${dni}, name: ${evt.name} value: ${evt.value}"
		sendHubCommand(new physicalgraph.device.HubAction(msg, physicalgraph.device.Protocol.LAN, "${ip}:${port}"))
    }
}

def sendSetup() {
    def thisMsg = ""
    presenceDevices.each {thisMsg = thisMsg + "p:$it.displayName:stHub_$it.deviceNetworkId\n"}
    motionDevices.each {thisMsg = thisMsg + "m:$it.displayName:stHub_$it.deviceNetworkId\n"}
    contactDevices.each {thisMsg = thisMsg + "c:$it.displayName:stHub_$it.deviceNetworkId\n"}
    accelerationDevices.each {thisMsg = thisMsg + "a:$it.displayName:stHub_$it.deviceNetworkId\n"}
    multiSensors.each {thisMsg = thisMsg + "x:$it.displayName:stHub_$it.deviceNetworkId\n"}
    omniSensors.each {thisMsg = thisMsg + "o:$it.displayName:stHub_$it.deviceNetworkId\n"}
    switchDevices.each {thisMsg = thisMsg + "s:$it.displayName:stHub_$it.deviceNetworkId\n"}
    dimmerDevices.each {thisMsg = thisMsg + "d:$it.displayName:stHub_$it.deviceNetworkId\n"}
    def dni = "systemHubLink"    
def msg = """POST / HTTP/1.1
HOST: ${ip}:${port}
CONTENT-TYPE: text/plain
DEVICE-NETWORK-ID: ${dni}
CONTENT-LENGTH: ${thisMsg.length()}

${thisMsg}
"""
    if(enabled) {
    	if (logEnable) log.debug "Setup: $msg"
		sendHubCommand(new physicalgraph.device.HubAction(msg, physicalgraph.device.Protocol.LAN, "${ip}:${port}"))
	}    
}

def uninstalled() {
	removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}
