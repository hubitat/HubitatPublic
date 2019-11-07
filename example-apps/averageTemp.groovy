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
			paragraph "Enter weight factors and offsets"
			tempSensors.each {
				input "weight$it.id", "decimal", title: "$it ($it.currentTemperature)", defaultValue: 1.0, submitOnChange: true, width: 3
				input "offset$it.id", "decimal", title: "$it Offset", defaultValue: 0.0, submitOnChange: true, range: "*..*", width: 3
			}
			input "useRun", "number", title: "Compute running average over this many sensor events:", defaultValue: 1, submitOnChange: true
			if(tempSensors) paragraph "Current sensor average is ${averageTemp()}°"
			if(useRun > 1) {
				initRun()
				if(tempSensors) paragraph "Current running average is ${averageTemp(useRun)}°"
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
	def averageDev = getChildDevice("AverageTemp_${app.id}")
	if(!averageDev) averageDev = addChildDevice("hubitat", "Virtual Temperature Sensor", "AverageTemp_${app.id}", null, [label: thisName, name: thisName])
	averageDev.setTemperature(averageTemp())
	subscribe(tempSensors, "temperature", handler)
}

def initRun() {
	def temp = averageTemp()
	if(!state.run) {
		state.run = []
		for(int i = 0; i < useRun; i++) state.run += temp
	}
}

def averageTemp(run = 1) {
	def total = 0
	def n = 0
	tempSensors.each {
		def offset = settings["offset$it.id"] != null ? settings["offset$it.id"] : 0
		total += (it.currentTemperature + offset) * (settings["weight$it.id"] != null ? settings["weight$it.id"] : 1)
		n += settings["weight$it.id"] != null ? settings["weight$it.id"] : 1
	}
	def result = total / (n = 0 ? tempSensors.size() : n)
	if(run > 1) {
		total = 0
		state.run.each {total += it}
		result = total / run
	}
	return result.toDouble().round(1)
}

def handler(evt) {
	def averageDev = getChildDevice("AverageTemp_${app.id}")
	def avg = averageTemp()
	if(useRun > 1) {
		state.run = state.run.drop(1) + avg
		avg = averageTemp(useRun)
	}
	averageDev.setTemperature(avg)
	log.info "Average sensor temperature = ${averageTemp()}°" + (useRun > 1 ? "    Running average is $avg°" : "")
}
