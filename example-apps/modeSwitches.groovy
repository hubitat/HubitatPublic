/**
 *  Mode Switches
 *
 *  Copyright 2023 Hubitat, Inc.  All Rights Reserved.
 *
 */

definition(
	name: "Mode Switches",
	namespace: "hubitat",
	author: "Bruce Ravenel",
	description: "Control Switches by Mode",
	category: "Convenience",
	iconUrl: "",
	iconX2Url: ""
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
	if(!state.modeSwitch) state.modeSwitch = [:]
	dynamicPage(name: "mainPage", title: "Mode Switch Table", uninstall: true, install: true) {
		section {
			input "lights", "capability.switch", title: "Select Switches to Control", multiple: true, submitOnChange: true, width: 4
			if(lights) {
				lights.each{dev ->
					if(!state.modeSwitch[dev.id]) state.modeSwitch[dev.id] = [:]
					location.modes.each{if(!state.modeSwitch[dev.id][it.name]) state.modeSwitch[dev.id][it.name] = " "}
				}
				paragraph displayTable()
				input "logging", "bool", title: "Enable Logging?", defaultValue: true, submitOnChange: true
			}
		}
	}
}

String displayTable() {
	String str = "<script src='https://code.iconify.design/iconify-icon/1.0.0/iconify-icon.min.js'></script>"
	str += "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px }" +
		"</style><div style='overflow-x:auto'><table class='mdl-data-table tstat-col' style=';border:2px solid black'>" +
		"<thead><tr style='border-bottom:2px solid black'><th style='border-right:2px solid black'>Switch</th>"
	List modes = location.modes?.clone()
	modes.sort{it.name}.each{str += "<th colspan='2'>$it.name</th>"}
	str += "</tr><tr style='border-bottom:2px solid black'><td style='border-right:2px solid black'> </td>"
	modes.each{str += "<th>On</th><th>Off</th>"}
	str += "</tr></thead>"
	String X = "<i class='he-checkbox-checked'></i>"
	String O = "<i class='he-checkbox-unchecked'></i>"
	lights.sort{it.displayName.toLowerCase()}.each {dev ->
		String devLink = "<a href='/device/edit/$dev.id' target='_blank' title='Open Device Page for $dev'>$dev<span style='color:black'>($dev.currentSwitch)</span>"
		str += "<tr style='color:black'><td style='border-right:2px solid black'>$devLink</td>"
		modes.sort{it.name}.each{
			str += "<td>${buttonLink("$dev.id:$it.name:on", state.modeSwitch[dev.id][it.name] == "on" ? X : O, "#1A77C9")}</td>"
			str += "<td>${buttonLink("$dev.id:$it.name:off", state.modeSwitch[dev.id][it.name] == "off" ? X : O, "#1A77C9")}</td>"
		}
	}
	str += "</tr></table></div>"
	str
}

String buttonLink(String btnName, String linkText, color = "#1A77C9", font = "15px") {
	"<div class='form-group'><input type='hidden' name='${btnName}.type' value='button'></div><div><div class='submitOnChange' onclick='buttonClick(this)' style='color:$color;cursor:pointer;font-size:$font'>$linkText</div></div><input type='hidden' name='settings[$btnName]' value=''>"
}

void appButtonHandler(btn) {
	List b = btn.tokenize(":")
	String s = state.modeSwitch[b[0]][b[1]]
	state.modeSwitch[b[0]][b[1]] = s == " " || s != b[2] ? b[2] : " "
}

def updated() {
	unsubscribe()
	initialize()
}

def installed() {
	initialize()
}

void initialize() {
	subscribe(location, "mode", modeHandler)
}

void modeHandler(evt) {
	if(logging) log.info "Mode is now <b>$evt.value</b>"
	lights.each{dev -> 
		String s = state.modeSwitch[dev.id][evt.value]
		if(s != " ") {
			dev."$s"()
			if(logging) log.info "$dev turned $s"
		}
	}
}
