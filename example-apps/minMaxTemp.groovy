definition(
    name: "Min/Max Temperatures",
    namespace: "hubitat",
    author: "Bruce Ravenel",
    description: "Return min/max of temperature sensors",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "")

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			input "thisName", "text", title: "Name this min/max temperature setter", submitOnChange: true
			if(thisName) app.updateLabel("$thisName")
			input "tempSensors", "capability.temperatureMeasurement", title: "Select Temperature Sensors", submitOnChange: true, required: true, multiple: true
			if(tempSensors) {
				tempRes = minTemp()
				paragraph "Current minimum sensor is $tempRes.temp° on $tempRes.dev"
				Map tempRes = maxTemp()
				paragraph "Current maximum sensor is $tempRes.temp° on $tempRes.dev"
			}
		}
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	def minDev = getChildDevice("MinTemp_${app.id}")
	if(!minDev) minDev = addChildDevice("hubitat", "Virtual Temperature Sensor", "MinTemp_${app.id}", null, [label: "${thisName}-Min", name: "${thisName}-Min"])
	def maxDev = getChildDevice("MaxTemp_${app.id}")
	if(!maxDev) maxDev = addChildDevice("hubitat", "Virtual Temperature Sensor", "MaxTemp_${app.id}", null, [label: "${thisName}-Max", name: "${thisName}-Max"])
	Map tempRes = minTemp()
	minDev.setTemperature(tempRes.temp)
	log.info "Current minimum sensor is $tempRes.temp° on $tempRes.dev"
	tempRes = maxTemp()
	maxDev.setTemperature(tempRes.temp)
	log.info "Current maximum sensor is $tempRes.temp° on $tempRes.dev"
	subscribe(tempSensors, "temperature", handler)
}

Map maxTemp() {
	Map result = [temp: -1000, dev: ""]
	tempSensors.each{
		if(it.currentTemperature > result.temp) {
			result.temp = it.currentTemperature
			result.dev = it.displayName
		}
	}
	return result
}

Map minTemp() {
	Map result = [temp: 1000, dev: ""]
	tempSensors.each{
		if(it.currentTemperature < result.temp) {
			result.temp = it.currentTemperature
			result.dev = it.displayName
		}
	}
	return result
}

def handler(evt) {
	def minDev = getChildDevice("MinTemp_${app.id}")
	def maxDev = getChildDevice("MaxTemp_${app.id}")
	Map res = minTemp()
	minDev.setTemperature(res.temp)
	log.info "Current minimum sensor is $res.temp° on $res.dev"
	res = maxTemp()
	maxDev.setTemperature(res.temp)
	log.info "Current maximum sensor is $res.temp° on $res.dev"
}
