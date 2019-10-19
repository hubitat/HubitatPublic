definition(
    name: "Average Illuminance",
    namespace: "hubitat",
    author: "Bruce Ravenel",
    description: "Average some illuminance sensors",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "")

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			input "thisName", "text", title: "Name this illuminance averager", submitOnChange: true
			if(thisName) app.updateLabel("$thisName")
			input "luxSensors", "capability.illuminanceMeasurement", title: "Select Illuminance Sensors", submitOnChange: true, required: true, multiple: true
			if(luxSensors) paragraph "Current average is ${averageLux()} lux"
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
	def averageDev = getChildDevice("AverageLux_${app.id}")
	if(!averageDev) averageDev = addChildDevice("hubitat", "Virtual Illuminance Sensor", "AverageLux_${app.id}", null, [label: thisName, name: thisName])
	averageDev.setLux(averageLux())
	subscribe(luxSensors, "illuminance", handler)
}

def averageLux() {
	def total = 0
	def n = luxSensors.size()
	luxSensors.each {total += it.currentIlluminance}
	return (total / n).toDouble().round(0).toInteger()
}

def handler(evt) {
	def averageDev = getChildDevice("AverageLux_${app.id}")
	def avg = averageLux()
	averageDev.setLux(avg)
	log.info "Average illuminance = $avg lux"
}
