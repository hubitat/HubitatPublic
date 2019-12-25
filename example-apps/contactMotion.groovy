definition(
    name: "Contact-Motion",
    namespace: "hubitat",
    author: "Bruce Ravenel",
    description: "Turn Contact Sensor into Motion Sensor",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "")

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			input "thisName", "text", title: "Name this Contact-Motion", submitOnChange: true
			if(thisName) app.updateLabel("$thisName")
			input "contactSensors", "capability.contactSensor", title: "Select Contact Sensors", submitOnChange: true, required: true, multiple: true
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
	def motionDev = getChildDevice("ContactMotion_${app.id}")
	if(!motionDev) motionDev = addChildDevice("hubitat", "Virtual Motion Sensor", "ContactMotion_${app.id}", null, [label: thisName, name: thisName])
	subscribe(contactSensors, "contact", handler)
}

def handler(evt) {
	def motionDev = getChildDevice("ContactMotion_${app.id}")
	if(evt.value == "open") motionDev.active() else motionDev.inactive()
	log.info "Contact $evt.device $evt.value"
}
