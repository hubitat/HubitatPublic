definition(
    name: "Switch-Lock",
    namespace: "hubitat",
    author: "Bruce Ravenel",
    description: "Use Switch to Lock/Unlock",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "")

preferences {
	page(name: "mainPage")
}

Map mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			input "thisName", "text", title: "Name this Switch-Lock", submitOnChange: true
			if(thisName) app.updateLabel("$thisName")
			input "lock", "capability.lock", title: "Select Lock", submitOnChange: true, required: true
			input "lockOnly", "bool", title: "Lock only?", submitOnChange: true, width: 3
			input "unlockOnly", "bool", title: "Unock only?", submitOnChange: true, width: 3
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

void initialize() {
	def switchDev = getChildDevice("SwitchLock_${app.id}")
	if(!switchDev) switchDev = addChildDevice("hubitat", "Room Lights Activator Switch", "SwitchLock_${app.id}", null, [label: lock.label, name: lock.name])
	subscribe(switchDev, "switch", handler)
}

void handler(evt) {
	def switchDev = getChildDevice("SwitchLock_${app.id}")
	if(evt.value == "on") if(!unlockOnly) {lock.lock(); log.info "$lock locked"}
	else if(!lockOnly) {lock.unlock(); log.info "$lock unlocked"}
	if(lockOnly) switchDev.soff()
	if(unlockOnly) switchDev.son()
}

void on() {}
void off() {}
