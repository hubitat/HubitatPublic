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
			paragraph "Enter weight factors"
			tempSensors.each {input "weight$it.id", "decimal", title: "$it ($it.currentTemperature)", defaultValue: 1.0, submitOnChange: true, width: 3}
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
	def n = 0
	tempSensors.each {
		total += it.currentTemperature * (settings["weight$it.id"] != null ? settings["weight$it.id"] : 1)
		n += settings["weight$it.id"] != null ? settings["weight$it.id"] : 1
	}
	return (total / (n = 0 ? tempSensors.size() : n)).toDouble().round(1)
}

def handler(evt) {
	def averageDev = getChildDevice("AverageTemp_${app.id}")
	def avg = averageTemp()
	averageDev.setTemperature(avg)
	log.info "Average temperature = $avg°"
}

