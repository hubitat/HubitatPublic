definition(
    name: "Average Temperatures",
    namespace: "hubitat",
    author: "Bruce Ravenel",
    description: "Average some temperature sensors",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "")

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			input "thisName", "text", title: "Name this temperature averager", submitOnChange: true
			if(thisName) app.updateLabel("$thisName")
			input "tempSensors", "capability.temperatureMeasurement", title: "Select Temperature Sensors", submitOnChange: true, required: true, multiple: true
			if(tempSensors) paragraph "Current average is ${averageTemp()}°"
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
	def averageDev = getChildDevice("AverageTemp_${app.id}")
	if(!averageDev) averageDev = addChildDevice("hubitat", "Virtual Temperature Sensor", "AverageTemp_${app.id}", null, [label: thisName, name: thisName])
	averageDev.setTemperature(averageTemp())
	subscribe(tempSensors, "temperature", handler)
}

def averageTemp() {
	def total = 0
	def n = tempSensors.size()
	tempSensors.each {total += it.currentTemperature}
	return (total / n).toDouble().round(1)
}

def handler(evt) {
	def averageDev = getChildDevice("AverageTemp_${app.id}")
	def avg = averageTemp()
	averageDev.setTemperature(avg)
	log.info "Average temperature = $avg°"
}
