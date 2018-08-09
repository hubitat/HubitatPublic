/*
	Environment Sensor

	2018-08-09 maxwell
		-cleaned up parsing and eventing
*/

import groovy.transform.Field

@Field Map diagAttributes = [
    	"0000":["name":"ResetCount","val":0x0000],
    	"0104":["name":"TXRetrys","val":0x0104],
    	"0105":["name":"TXFails","val":0x0105],
    	"011A":["name":"PacketDrops","val":0x011A],
    	"0115":["name":"DecryptFailures","val":0x0115],
    	"011D":["name":"RSSI","val":0x011D],
    	"011E":["name":"Parent","val":0x011E],
    	"011F":["name":"Children","val":0x011F],
    	"0120":["name":"Neighbors","val":0x0120]
    ]

metadata {
    definition (name: "Environment Sensor", namespace: "iharyadi", author: "iharyadi/maxwell") {
	capability "Configuration"
	capability "Refresh"
	capability "Temperature Measurement"
	capability "RelativeHumidityMeasurement"
	capability "Illuminance Measurement"
	capability "PressureMeasurement"
	capability "Sensor"
	capability "Switch"

        fingerprint profileId: "0104", inClusters: "0000,0003,0006,0402,0403,0405,0400,0B05", manufacturer: "KMPCIL", model: "RES001BME280", deviceJoinName: "Environment Sensor"
/*
Manufacturer: KMPCIL
Product Name: Environment Sensor
Model Number: RES001BME280
deviceTypeId: 1373
manufacturer:KMPCIL
address64bit:00124B00179E42B8
address16bit:117B
model:RES001BME280 
basicAttributesInitialized:true
application:01
endpoints.08.manufacturer:KMPCIL
endpoints.08.idAsInt:8
endpoints.08.inClusters:0000,0003,0006,0402,0403,0405,0400,0B05
endpoints.08.endpointId:08
endpoints.08.profileId:0104
endpoints.08.application:01
endpoints.08.outClusters:0000
endpoints.08.initialized:true
endpoints.08.model:RES001BME280 
endpoints.08.stage:4
endpoints.F2.manufacturer:null
endpoints.F2.idAsInt:242
endpoints.F2.inClusters:null
endpoints.F2.endpointId:F2
endpoints.F2.profileId:A1E0
endpoints.F2.application:null
endpoints.F2.outClusters:0021
endpoints.F2.initialized:true
endpoints.F2.model:null
endpoints.F2.stage:4
*/
	}
        
    preferences {
        //standard logging options
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        
        //TODO: implement sensor adjustments, suggest doing so using reference values vs offsets
        //input "refTemp", "decimal", title: "Reference temperature", description: "Enter current reference temperature reading", range: "*..*"
        
        //input "tempOffset", "decimal", title: "Degrees", description: "Adjust temperature by this many degrees in Celcius",range: "*..*"
        //input "tempFilter", "decimal", title: "Coeficient", description: "Temperature filter between 0.0 and 1.0",range: "*..*"
		//input "humOffset", "decimal", title: "Percent", description: "Adjust humidity by this many percent",range: "*..*"
        //input "illumAdj", "decimal", title: "Factor", description: "Adjust illuminace base on formula illum / Factor", range: "1..*"
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def parse(String description) {
	if (logEnable) log.debug "description is ${description}"
	if (description.startsWith("catchall")) return
	def descMap = zigbee.parseDescriptionAsMap(description)
	if (logEnable) log.debug "descMap:${descMap}"
	
	def cluster = descMap.cluster
	def hexValue = descMap.value
	def attrId = descMap.attrId
	
	switch (cluster){
		case "0400" :	//illuminance
			getLuminanceResult(hexValue)
			break
		case "0402" :	//temp
			getTemperatureResult(hexValue)
			break
		case "0403" :	//pressure
			getPressureResult(hexValue)
			break
		case "0405" :	//humidity
			getHumidityResult(hexValue)
			break
		case "0B05" : //diag
        		if (logEnable) log.warn "attrId:${attrId}, hexValue:${hexValue}"
        		def value = hexStrToUnsignedInt(hexValue)
        		log.warn "diag- ${diagAttributes."${attrId}".name}:${value} "
			break
		default :
			log.warn "skipped cluster: ${cluster}, descMap:${descMap}"
			break
	}
	return
}

//event methods
private getTemperatureResult(hex){
    def valueRaw = hexStrToSignedInt(hex)
    valueRaw = valueRaw / 100
    def value = convertTemperatureIfNeeded(valueRaw.toFloat(),"c",1)
    /*
	//example temp offset
	state.sensorTemp = value
    if (state.tempOffset) {
        value =  (value.toFloat() + state.tempOffset.toFloat()).round(2)
    }
	*/
    def name = "temperature"
    def unit = "°${location.temperatureScale}"
    def descriptionText = "${device.displayName} ${name} is ${value}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: name,value: value,descriptionText: descriptionText,unit: unit)
}

private getHumidityResult(hex){
    def valueRaw = hexStrToUnsignedInt(hex)
    def value = valueRaw / 100
    def name = "humidity"
    def unit = "%"
    def descriptionText = "${device.displayName} ${name} is ${value}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: name,value: value,descriptionText: descriptionText,unit: unit)
}

private getLuminanceResult(hex) {
	def rawValue = hexStrToUnsignedInt(hex)
    def value = (Math.pow(10,(rawValue/10000))+ 1).toInteger()
    if (rawValue.toInteger() == 0) value = "0"
    def name = "illuminance"
    def unit = "Lux"	
    def descriptionText = "${device.displayName} ${name} is ${value}${unit}"
    if (txtEnable) log.info "${descriptionText}"
	sendEvent(name: name,value: value,descriptionText: descriptionText,unit: unit)
}

private getPressureResult(hex){
    def valueRaw = hexStrToUnsignedInt(hex)
    def value = valueRaw / 10
    def name = "pressure"
    def unit = "kPa"
    def descriptionText = "${device.displayName} ${name} is ${value}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: name,value: value,descriptionText: descriptionText,unit: unit)
}


//capability and device methods
def off() {
    zigbee.off()
}

def on() {
    zigbee.on()
}

def refresh() {
    log.debug "Refresh"
    
	//readAttribute(cluster,attribute,mfg code,optional delay ms)
    def cmds = zigbee.readAttribute(0x0402,0x0000,[:],200) +		//temp
        zigbee.readAttribute(0x0405,0x0000,[:],200) + 			//humidity
        zigbee.readAttribute(0x0403,0x0000,[:],200) +			//pressure
        zigbee.readAttribute(0x0400,0x0000,[:],200) 			//illuminance
    	diagAttributes.each{ it ->
            //log.debug "it:${it.value.val}"
			cmds +=  zigbee.readAttribute(0x0B05,it.value.val,[:],200) 
		}  
    return cmds
}

def configure() {
    log.debug "Configuring Reporting and Bindings."
    runIn(1800,logsOff)
    
    //temp offset init
    //state.tempOffset = 0
    
    List cmds = zigbee.temperatureConfig(5,300)												//temp
    cmds = cmds + zigbee.configureReporting(0x0405, 0x0000, DataType.UINT16, 5, 300, 100)	//humidity
    cmds = cmds + zigbee.configureReporting(0x0403, 0x0000, DataType.UINT16, 5, 300, 2)		//pressure
    cmds = cmds + zigbee.configureReporting(0x0400, 0x0000, DataType.UINT16, 1, 300, 500)	//illuminance
    cmds = cmds + refresh()
    log.info "cmds:${cmds}"
    return cmds
}

def updated() {
    log.trace "Updated()"
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)    

    /* example temp offset
   	def crntTemp = device?.currentValue("temperature")
    if (refTemp && crntTemp && state.sensorTemp) {
        def prevOffset = (state.tempOffset ?: 0).toFloat().round(2)
        def deviceTemp = state.sensorTemp.toFloat().round(2)
        def newOffset =  (refTemp.toFloat() - deviceTemp).round(2)
        def newTemp = (deviceTemp + newOffset).round(2)
        //send new event on offSet change
        if (newOffset.toString() != prevOffset.toString()){
            state.tempOffset = newOffset
            def map = [name: "temperature", value: "${newTemp}", descriptionText: "${device.displayName} temperature offset was set to ${newOffset}°${location.temperatureScale}"]
            if (txtEnable) log.info "${map.descriptionText}"
            sendEvent(map)
        }
        //clear refTemp so it doesn't get changed later...
        device.removeSetting("refTemp")
    }
	*/
}
