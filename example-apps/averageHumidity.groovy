definition(
    name: "Average Humidity",
    namespace: "hubitat",
    author: "Bruce Ravenel",
    description: "Average some humidity sensors",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "")

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			input "thisName", "text", title: "Name this humidity averager", submitOnChange: true
			if(thisName) app.updateLabel("$thisName")
			input "humidSensors", "capability.relativeHumidityMeasurement", title: "Select Humidity Sensors", submitOnChange: true, required: true, multiple: true
			if(humidSensors) paragraph "Current average is ${averageHumid()}%"
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
	def averageDev = getChildDevice("AverageHumid_${app.id}")
	if(!averageDev) averageDev = addChildDevice("hubitat", "Virtual Humidity Sensor", "AverageHumid_${app.id}", null, [label: thisName, name: thisName])
	averageDev.setHumidity(averageHumid())
	subscribe(humidSensors, "humidity", handler)
}

def averageHumid() {
	def total = 0
	def n = humidSensors.size()
	humidSensors.each {total += it.currentHumidity}
	return (total / n).toDouble().round(1)
}

def handler(evt) {
	def averageDev = getChildDevice("AverageHumid_${app.id}")
	def avg = averageHumid()
	averageDev.setHumidity(avg)
	log.info "Average humidity = $avg%"
}
