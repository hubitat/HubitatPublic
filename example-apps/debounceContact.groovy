definition(
    name: "Debounce contact",
    namespace: "hubitat",
    author: "Bruce Ravenel",
    description: "Debounce double reporting contact sensor",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "")

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			input "thisName", "text", title: "Name this debouncer; debounce switch will have this name", submitOnChange: true
			if(thisName) app.updateLabel("$thisName")
			input "contact", "capability.contactSensor", title: "Select Contact Sensor", submitOnChange: true, required: true
			input "openClose", "bool", title: "Turn this on for closed, open is default (switch turns on for open)", submitOnChange: true
			input "delayTime", "number", title: "Enter number of milliseconds to delay for debounce", submitOnChange: true, defaultValue: 1000
		}
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	def debounceDev = getChildDevice("debounceSwitch_${app.id}")
	if(!debounceDev) debounceDev = addChildDevice("hubitat", "Virtual Switch", "debounceSwitch_${app.id}", null, [label: thisName, name: thisName])
	debounceDev.off()
	subscribe(contact, "contact", handler)
}

def handler(evt) {
	runInMillis(delayTime, debounced, [data: [o: evt.value]])
	log.info "Contact $evt.device $evt.value, start delay of $delayTime milliseconds"
}

def debounced(data) {
	def onOff = openClose ? data.o == "closed" : data.o == "open"
	log.info "Debounced switch " + (onOff ? "on" : "off")
	def debounceDev = getChildDevice("debounceSwitch_${app.id}")
	if(onOff) debounceDev.on() else debounceDev.off()
}
