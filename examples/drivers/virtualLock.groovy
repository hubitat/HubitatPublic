/*
    Copyright 2016, 2017, 2018 Hubitat Inc.  All Rights Reserved

    virtual lock with lock codes for testing new lockCode capabilities

    2018-07-08 maxwell
        -add encryption support
		-add extended comments for community

*/
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Virtual Lock", namespace: "hubitat", author: "Mike Maxwell") {
        capability "Actuator"
        capability "Lock"
        capability "Lock Codes"
        //test commands
        command "testSetMaxCodes", ["NUMBER"]
        command "testUnlockWithCode", ["NUMBER"]
    }

    preferences{
        input name: "optEncrypt", type: "bool", title: "Enable lockCode encryption", defaultValue: false, description: ""
        //standard logging options for all drivers
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false, description: ""
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true, description: ""
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed(){
    log.warn "installed..."
    sendEvent(name:"maxCodes",value:20)
    sendEvent(name:"codeLength",value:4)
    lock()
}

def updated() {
    log.info "updated..."
    log.warn "description logging is: ${txtEnable == true}"
    log.warn "encryption is: ${optEncrypt == true}"
    //check crnt lockCodes for encryption status
    updateEncryption()
    //turn off debug logs after 30 minutes
    if (logEnable) runIn(1800,logsOff)
}

//handler for hub to hub integrations
def parse(String description) {
    if (logEnable) log.debug "parse ${description}"
    if (description == "locked") lock()
    else if (description == "unlocked") unlock()
}

//capability commands
def lock(){
    def descriptionText = "${device.displayName} was locked"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:"lock",value:"locked",descriptionText: descriptionText, type:"digital")
}

def unlock(){
    /*
    on sucess event
        name	value								data
        lock	unlocked | unlocked with timeout	[<codeNumber>:[code:<pinCode>, name:<display name for code>]]
    */
    def descriptionText = "${device.displayName} was unlocked [digital]"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:"lock",value:"unlocked",descriptionText: descriptionText, type:"digital")
}

def setCodeLength(length){
    /*
	on install/configure/change
		name		value
		codeLength	length
	*/
    def descriptionText = "${device.displayName} codeLength set to ${length}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:"codeLength",value:length,descriptionText:descriptionText)
}

def setCode(codeNumber, code, name = null) {
    /*
	on sucess
		name		value								data												notes
		codeChanged	added | changed						[<codeNumber>":["code":"<pinCode>", "name":"<display name for code>"]]	default name to code #<codeNumber>
		lockCodes	JSON list of all lockCode
	*/
    if (!name) name = "code #${codeNumber}"

    def lockCodes = getLockCodes()
    def codeMap = getCodeMap(lockCodes,codeNumber)
    def data = [:]
    def value
    //verify proposed changes
    if (!changeIsValid(codeMap,codeNumber,code,name)) return

    if (logEnable) log.debug "setting code ${codeNumber} to ${code} for lock code name ${name}"

    if (codeMap) {
        if (codeMap.name != name || codeMap.code != code) {
            codeMap = ["name":"${name}", "code":"${code}"]
            lockCodes."${codeNumber}" = codeMap
            data = ["${codeNumber}":codeMap]
            if (optEncrypt) data = encrypt(JsonOutput.toJson(data))
            value = "changed"
        }
    } else {
        codeMap = ["name":"${name}", "code":"${code}"]
        data = ["${codeNumber}":codeMap]
        lockCodes << data
        if (optEncrypt) data = encrypt(JsonOutput.toJson(data))
        value = "added"
    }
    updateLockCodes(lockCodes)
    sendEvent(name:"codeChanged",value:value,data:data, isStateChange: true)
}

def deleteCode(codeNumber) {
    /*
	on sucess
		name		value								data
		codeChanged	deleted								[<codeNumber>":["code":"<pinCode>", "name":"<display name for code>"]]
		lockCodes	[<codeNumber>":["code":"<pinCode>", "name":"<display name for code>"],<codeNumber>":["code":"<pinCode>", "name":"<display name for code>"]]
	*/
    def codeMap = getCodeMap(lockCodes,"${codeNumber}")
    def result = [:]
    if (codeMap) {
        //build new lockCode map, exclude deleted code
        lockCodes.each{
            if (it.key != "${codeNumber}"){
                result << it
            }
        }
        updateLockCodes(result)
        def data =  ["${codeNumber}":codeMap]
        //encrypt lockCode data is requested
        if (optEncrypt) data = encrypt(JsonOutput.toJson(data))
        sendEvent(name:"codeChanged",value:"deleted",data:data, isStateChange: true)
    }
}

//virtual test methods
def testSetMaxCodes(length){
    //on a real lock this event is generated from the response to a configuration report request
    sendEvent(name:"maxCodes",value:length)
}

def testUnlockWithCode(code = null){
    if (logEnable) log.debug "testUnlockWithCode: ${code}"
    /*
	lockCodes in this context calls the helper function getLockCodes()
	*/
    def lockCode = lockCodes.find{ it.value.code == "${code}" }
    if (lockCode){
        def data = ["${lockCode.key}":lockCode.value]
        def descriptionText = "${device.displayName} was unlocked [physical]"
        if (txtEnable) log.info "${descriptionText}"
        if (optEncrypt) data = encrypt(JsonOutput.toJson(data))
        sendEvent(name:"lock",value:"unlocked",descriptionText: descriptionText, type:"physical",data:data)
    } else {
        if (txtEnable) log.debug "testUnlockWithCode failed with invalid code"
    }
}

//helpers
private changeIsValid(codeMap,codeNumber,code,name){
    //validate proposed lockCode change
    def result = true
    def codeLength = device.currentValue("codeLength")?.toInteger() ?: 4
    def maxCodes = device.currentValue("maxCodes")?.toInteger() ?: 20
    def isBadLength = codeLength != code.size()
    def isBadCodeNum = maxCodes < codeNumber
    //load lockCodes into a local variable since we're referencing it multiple times
    def lockCodes = getLockCodes()
    if (lockCodes) {
        def nameSet = lockCodes.collect{ it.value.name }
        def codeSet = lockCodes.collect{ it.value.code }
        if (codeMap) {
            nameSet = nameSet.findAll{ it != codeMap.name }
            codeSet = codeSet.findAll{ it != codeMap.code }
        }
        def nameInUse = name in nameSet
        def codeInUse = code in codeSet
        if (nameInUse || codeInUse) {
            if (logEnable && nameInUse) { log.warn "changeIsValid:false, name:${name} is in use:${ lockCodes.find{ it.value.name == "${name}" } }" }
            if (logEnable && codeInUse) { log.warn "changeIsValid:false, code:${code} is in use:${ lockCodes.find{ it.value.code == "${code}" } }" }
            result = false
        }
    }
    if (isBadLength || isBadCodeNum) {
        if (logEnable && isBadLength) { log.warn "changeIsValid:false, length of code ${code} does not match codeLength of ${codeLength}" }
        if (logEnable && isBadCodeNum) { log.warn "changeIsValid:false, codeNumber ${codeNumber} is larger than maxCodes of ${maxCodes}" }
        result = false
    }
    return result
}

private getCodeMap(lockCodes,codeNumber){
    def codeMap = [:]
    def lockCode = lockCodes?."${codeNumber}"
    if (lockCode) {
        codeMap = ["name":"${lockCode.name}", "code":"${lockCode.code}"]
    }
    return codeMap
}

private getLockCodes() {
    /*
	on a real lock we would fetch these from the response to a configuration report request
	*/
    def lockCodes = device.currentValue("lockCodes")
    def result = [:]
    if (lockCodes) {
        //decrypt codes if they're encrypted
        if (lockCodes[0] == "{") result = new JsonSlurper().parseText(lockCodes)
        else result = new JsonSlurper().parseText(decrypt(lockCodes))
    }
    return result
}

private updateLockCodes(lockCodes){
    /*
	whenever a code changes we update the lockCodes event
	*/
    if (logEnable) log.debug "updateLockCodes: ${lockCodes}"
    def data = new groovy.json.JsonBuilder(lockCodes)
    if (optEncrypt) data = encrypt(data.toString())
    sendEvent(name:"lockCodes",value:data)
}

private updateEncryption(){
    /*
	resend lockCodes map when the encryption option is changed
	*/
    def lockCodes = device.currentValue("lockCodes") //encrypted or decrypted
    if (lockCodes){
        if (optEncrypt && lockCodes[0] == "{") {	//resend encrypted
            sendEvent(name:"lockCodes",value: encrypt(lockCodes))
        } else if (!optEncrypt && lockCodes[0] != "{") {	//resend decrypted
            sendEvent(name:"lockCodes",value: decrypt(lockCodes))
        }
    }
}
