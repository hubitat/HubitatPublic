definition(
	name: "All Off",
	namespace: "hubitat",
	author: "Bruce Ravenel",
	description: "Turn Devices Off with Recheck",
	category: "Convenience",
	iconUrl: "",
	iconX2Url: ""
)

preferences {
	page(name: "mainPage")
}

Map mainPage() {
	dynamicPage(name: "mainPage", title: "All Off", uninstall: true, install: true) {
		section {
			input "switches", "capability.switch", title: "Switches to turn off", multiple: true
			paragraph "For the trigger use a Virtual Switch with auto-off enabled, turning it on fires the main off command for the switches above"
			input "trigger", "capability.switch", title: "Trigger switch"
			input "retry", "number", title: "Select retry interval in seconds (default 1 second)", defaultValue: 1, submitOnChange: true, width: 4
			input "maxRetry", "number", title: "Maximum number of retries?", defaultValue: 5, submitOnChange: true, width: 4
			input "meter", "number", title: "Use metering (in milliseconds)", width: 4
		}
	}
}

void updated() {
	unsubscribe()
	initialize()
}

void installed() {
	initialize()
}

void initialize() {
	subscribe(trigger, "switch.off", handler)
	subscribe(switches, "switch.on", onHandler)
	atomicState.someOn = true
}

void handler(evt) {
	atomicState.retry = 0
	turnOff()
}

void turnOff() {
	if(atomicState.someOn) {
		Boolean maybeOn = false
		List whichOff = []
		switches.each{
			if(it.currentSwitch == "on") {
				it.off() 
				maybeOn = true
				whichOff += it
				if(meter) pause(meter)
			}
		}
		atomicState.someOn = maybeOn
		if(maybeOn) {
			log.info "Switches sent off commands: ${"$whichOff" - "[" - "]"}"
			atomicState.retry++
			if(atomicState.retry < maxRetry) runIn(retry, turnOff)
			else log.info "Stopped after $maxRetry attempts: ${"$whichOff" - "[" - "]"} still on"
		} else log.info "All switches reported off"
	}
}

void onHandler(evt) {
	atomicState.someOn = true
}
