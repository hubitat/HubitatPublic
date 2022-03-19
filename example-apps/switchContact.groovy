definition(
    name: "Switch-Contact",
    namespace: "hubitat",
    author: "Bruce Ravenel",
    description: "Turn a Switch into a Contact Sensor",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "")

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			input "thisName", "text", title: "Name this Switch-Contact", submitOnChange: true
			if(thisName) app.updateLabel("$thisName")
			input "switches", "capability.switch", title: "Select Switches", submitOnChange: true, required: true, multiple: true
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
	def contactDev = getChildDevice("SwitchContact_${app.id}")
	if(!contactDev) contactDev = addChildDevice("hubitat", "Virtual Contact Sensor", "SwitchContact_${app.id}", null, [label: thisName, name: thisName])
	subscribe(switches, "switch", handler)
}

def handler(evt) {
	def contactDev = getChildDevice("SwitchContact_${app.id}")
	if(evt.value == "on") contactDev.open() else contactDev.close()
	log.info "Switch $evt.device $evt.value"
}
