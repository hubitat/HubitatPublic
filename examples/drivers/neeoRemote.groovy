/*
	NEEO Remote

	Copyright 2016, 2017, 2018 Hubitat Inc.  All Rights Reserved

  2018-12-27 2.0.3 maxwell
    -add component switch option
    -implement set and clear for neeo brain forwarding
  2018-12-24 2.0.3 maxwell
    -initial pub

*/

metadata {
    definition (name: "NEEO Remote", namespace: "hubitat", author: "Mike Maxwell") {
        capability "MediaController"
        command "refresh"
    }
    preferences {
        input name: "ip", type: "text", title: "NEEO ip address"
        input name: "activityChildren", type: "bool", title: "Create Activity Switches", defaultValue: false
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:false,type:"bool"])
}

def updated(){
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
    if (ip) {
        def quad = ip.split(/\./)
        def hexIP = ""
        quad.each {
            hexIP+= Integer.toHexString(it.toInteger()).padLeft(2,"0").toUpperCase()
        }
        if (device.deviceNetworkId != hexIP) {
            device.setDeviceNetworkId(hexIP)
        }
        setForwarding()
        if (activityChildren) {
            deviceSync(state.activities)
        } else {
            getChildDevices().each { deleteChildDevice("${it.deviceNetworkId}") }
        }
        getAllActivities()
    } else log.error "no ip address set, please set an IP address."
}

private deviceSync(parsed) {
    def dni
    getChildDevices().each{
        dni = it.deviceNetworkId
        if (!(parsed.find{ it.value.scenerio == dni })) {
            deleteChildDevice("${dni}")
        }
    }
    parsed.each { d ->
        dni = "${d.value.scenerio}"
        def cd = getChildDevice("${dni}")
        if (!cd) {
            cd = addChildDevice("hubitat", "Generic Component Switch", "${dni}", [label: "${d.key}", name: "${d.key}", isComponent: true])
            if (cd && logEnable) {
                log.debug "Activity device ${cd.displayName} was created"
            } else if (!cd) {
                log.error "error creating device"
            }
        } //else log.info "device already exists"
    }
}

def installed() {
    log.debug "installed"
    device.updateSetting("txtEnable",[type:"bool", value: true])
}

def uninstalled(){
    log.trace "uninstalled"
    clearForwarding()
    getChildDevices().each { deleteChildDevice("${it.deviceNetworkId}") }
}

def startActivity(name) {
    def uri = state.activities."${name}"?.on
    if (uri) {
        generalGet(uri)
    }
}

private clearForwarding() {
    def params = [
            uri: "http://${ip}:3000",
            path: "/v1/forwardactions/",
            headers: [
                    "Content-Type": "application/json;charset=UTF-8"
            ],
            body: """{}"""
    ]
    generalPost(params)
}

private setForwarding() {
    def params = [
            uri: "http://${ip}:3000",
            path: "/v1/forwardactions/",
            headers: [
                    "Content-Type": "application/json;charset=UTF-8"
            ],
            body: """{"host":"${location.hubs[0].getDataValue("localIP")}","port":39501}"""
    ]
    generalPost(params)
}

private generalPost(params) {
    try{
        httpPost(params) { resp ->
            if (logEnable) log.debug "generalPost:${resp.data}"
        }
    } catch(e){
        log.error "generalPost error:${e}"
    }
}

private generalGet(uri){
    def params = [
            uri:uri
    ]
    try{
        httpGet(params) { resp ->
            if (logEnable) log.debug "generalGet:${resp.data}"
            if (resp.data?.estimatedDuration) state.execDelay = ((resp.data.estimatedDuration / 1000) + 1).toInteger()
        }
    } catch(e){
        log.error "generalGet error:${e}"
    }
}

def getCurrentActivity(){
    getAllActivities()
}

def getAllActivities(){
    def activities = [:]
    def currentActivity
    def params = [
            uri: "http://${ip}:3000"
            ,path: "/v1/api/recipes"
    ]
    try {
        httpGet(params) { resp ->
            resp.data.each{
                def name = URLDecoder.decode(it.detail.devicename)
                def actOn = it.url.setPowerOn
                def actOff = it.url.setPowerOff
                activities << ["${name}":["on":actOn,"off":actOff,"scenerio":it.powerKey]]
            }
            state.activities = activities
            sendEvent(name:"activities", value: new groovy.json.JsonBuilder(activities.collect{ it.key }) )
        }
    } catch(e){
        log.error "getAllActivities error:${e}"
    }

}

def parse(String description) {
    def msg = parseLanMessage(description)
    if (logEnable) log.debug "parse json:${msg.json}"
    if (msg.body) {
        def result = msg.json
        if (result.recipe){
            def action = result.action == "launch" ? "on" : "off"
            def activity = result.recipe
            if (activityChildren) {
                def cd = getChildDevice("${state.activities."${activity}".scenerio}")
                if (cd) cd.parse([[name:"switch",value:action,descriptionText:"Neeo activity ${activity} was turned ${action}"]])
            }
            if (action == "off") activity = "None"
            if (logEnable) log.trace "activity:${activity}, result.action:${result.action}, action:${action}"
            sendEvent(name:"currentActivity", value: activity)
            runIn(state.execDelay ?: 1,"refresh")
        }
    }
}

def refresh() {
    def cd
    def activeScenerios = []
    def inactiveScenerios = []
    def currentActivity = "None"
    def descriptionText = "${device.displayName} current activity is"
    def params = [ uri:"http://${ip}:3000/v1/api/activeRecipes" ]
    try{
        httpGet(params) { resp ->
            resp.data.each { act ->
                activeScenerios.add(act)
            }
        }
        inactiveScenerios = state.activities.collect{ it.value.scenerio } - activeScenerios
        inactiveScenerios.each {
            if (activityChildren) {
                cd = getChildDevice(it)
                if (cd && cd.currentValue("switch") != "off") {
                    cd.parse([[name:"switch",value:"off",descriptionText:"Neeo activity ${cd.displayName} was turned off"]])
                }
            }
        }
        activeScenerios.each { scenerio ->
            if (activityChildren) {
                cd = getChildDevice(scenerio)
                if (cd && cd.currentValue("switch") != "on") {
                    cd.parse([[name:"switch",value:"on",descriptionText:"Neeo activity ${cd.displayName} was turned on"]])
                }
            }
            currentActivity = state.activities.find{ it.value.scenerio == scenerio }.key
        }
        descriptionText = "${descriptionText} ${currentActivity}"
        if (txtEnable) log.info descriptionText
        sendEvent(name:"currentActivity", value: currentActivity, descriptionText: descriptionText)
    } catch(e){
        log.error "refresh error:${e}"
    }
}

def componentRefresh(cd) {
    refresh()
}

def componentOn(cd) {
    def uri = state.activities.find{ it.value.scenerio == cd.deviceNetworkId }.value.on
    if (uri) generalGet(uri)
}

def componentOff(cd) {
    def uri = state.activities.find{ it.value.scenerio == cd.deviceNetworkId }.value.off
    if (uri) generalGet(uri)
}
