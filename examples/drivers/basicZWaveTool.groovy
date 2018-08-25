/*
	Basic Z-Wave tool

	Copyright 2016, 2017, 2018 Hubitat Inc.  All Rights Reserved

	usage:
		-replace existing driver with this driver
		-set your paremeters
		-replace this driver with previous driver
	
*/

metadata {
    definition (name: "Basic Z-Wave tool",namespace: "hubitat", author: "Mike Maxwell") {
        
    	command "getCommandClassReport"
	command "getParameterReport", ["NUMBER"]
	command "setParameter", ["NUMBER","NUMBER","NUMBER"]
    }
}

def parse(String description) {
    def cmd = zwave.parse(description)
    if (cmd) {
        zwaveEvent(cmd)
    }
}
//Z-Wave responses
def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    def param = cmd.parameterNumber
    def value = cmd.scaledConfigurationValue
    def size = cmd.size
    log.info "ConfigurationReport- parameterNumber:${param}, size:${size}, value:${value}"
}

def zwaveEvent(hubitat.zwave.commands.versionv1.VersionCommandClassReport cmd) {
    def version = cmd.commandClassVersion
    def cmdClass = cmd.requestedCommandClass
    log.info "CommandClassReport- class:${ "0x${intToHexStr(cmdClass)}" }, version:${version}"		
}	

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapCmd = cmd.encapsulatedCommand()
    def result = []
    if (encapCmd) {
	result += zwaveEvent(encapCmd)
    } else {
        log.warn "Unable to extract encapsulated cmd from ${cmd}"
    }
    return result
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    log.debug "skip: ${cmd}"
}

//cmds
def setParameter(parameterNumber = null, size = null, value = null){
    if (parameterNumber == null || size == null || value == null) {
	log.warn "incomplete parameter list supplied..."
	log.info "syntax: setParameter(parameterNumber,size,value)"
    } else {
	return delayBetween([
	    secureCmd(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: parameterNumber, size: size)),
	    secureCmd(zwave.configurationV1.configurationGet(parameterNumber: parameterNumber))
	],500)
    }
}

def getParameterReport(param = null){
    def cmds = []
    if (param) {
	cmds = [secureCmd(zwave.configurationV1.configurationGet(parameterNumber: param))]
    } else {
	0.upto(255, {
	    cmds.add(secureCmd(zwave.configurationV1.configurationGet(parameterNumber: it)))	
	})
    }
    return cmds
}	

def getCommandClassReport(){
    def cmds = []
    def ic = getDataValue("inClusters").split(",").collect{ hexStrToUnsignedInt(it) }
    ic.each {
	if (it) cmds.add(secureCmd(zwave.versionV1.versionCommandClassGet(requestedCommandClass:it)))
    }
    return delayBetween(cmds,500)
}

def installed(){}

def configure() {}

def updated() {}

private secureCmd(cmd) {
    if (getDataValue("zwaveSecurePairingComplete") == "true") {
	return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
	return cmd.format()
    }	
}


