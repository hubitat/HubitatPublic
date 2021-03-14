metadata {
	definition (
		name: "Kasa Plug Hub Rebooter",
        namespace: "hubitat",
		author: "Victor U."
	) {
        command "schedulePowerCycle"
	}

	preferences {
		input ("device_IP", "text",
			   title: "Device IP",
			   defaultValue: getDataValue("deviceIP"))
		input ("debug", "bool",
			   title: "Enable debug logging",
			   defaultValue: false)
        input ("shutdownHub", "bool",
			   title: "Shut hub down (leave off for testing)",
			   defaultValue: false)
	}
}

def installed() {
	logInfo("Installing Device....")
	runIn(2, updated)
}

//	===== Updated and associated methods =====
def updated() {
    if (debug) log.debug("Updating device preferences....")
    
	unschedule()
    interfaces.rawSocket.disconnect()

	if (!device_IP) {
	    log.warn("Device IP is not set.")
		return
    } else {
		updateDataValue("deviceIP", device_IP.trim())
	}
}

void schedulePowerCycle() {
    if (!getDataValue("deviceIP")) {
        log.error("No Kasa plug IP specified")
        return
    }
    
    // TP-Link commands: https://github.com/softScheck/tplink-smartplug/blob/master/tplink-smarthome-commands.txt
    // JavaScript API docs: https://plasticrake.github.io/tplink-smarthome-api/
    log.info("scheduling a power cycle...")
    
    unschedule()
    resetPlugSchedule()
    pauseExecution(500)
    
    // calculate current minute of the day - that's what Kasa plugs schedule deals in
    Calendar calendar = new GregorianCalendar()
    int minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    log.info "scheduling plug to turn off at ${minutesToReadable(minuteOfDay+2)} and turn on at ${minutesToReadable(minuteOfDay+3)}..."
    
    // schedule command to power DOWN
    sendCmd(outputXOR("""{"schedule":{"add_rule":{"stime_opt":0,"wday":[1,1,1,1,1,1,1],"smin":${minuteOfDay+2},"enable":1,"repeat":1,"etime_opt":-1,"name":"lights off","eact":-1,"sact":0,"emin":0},"set_overall_enable":{"enable":1}}}"""))
    pauseExecution(500)
    
    // and another to power UP
    sendCmd(outputXOR("""{"schedule":{"add_rule":{"stime_opt":0,"wday":[1,1,1,1,1,1,1],"smin":${minuteOfDay+3},"enable":1,"repeat":1,"etime_opt":-1,"name":"lights on","eact":-1,"sact":1,"emin":0},"set_overall_enable":{"enable":1}}}"""))
    pauseExecution(500)
    
    // fetch the schedule to see that plug got the commands
    sendCmd(outputXOR("""{"schedule":{"get_rules":null}}"""))

    // don't reboot the hub tomorrow again
    runIn(600, "resetPlugSchedule")
    
    // shut the hub down
    if (shutdownHub) {
        httpPost([uri: "http://127.0.0.1:8080/hub/shutdown"]) {
            log.info "hub shutdown initiated"
        }
    }
}

private String minutesToReadable(int minutesOfDay) {
    java.text.DecimalFormat fmt = new java.text.DecimalFormat("00")
    return fmt.format(Math.floorDiv(minutesOfDay, 60)) + ":" + fmt.format(Math.floorMod(minutesOfDay, 60))
}

void resetPlugSchedule() {
    interfaces.rawSocket.disconnect()
    sendCmd(outputXOR("""{"schedule":{"delete_all_rules":null,"erase_runtime_stat":null}}"""))
}

private void sendCmd(String command) {
    if (!getDataValue("deviceIP")) {
        log.error("No Kasa plug IP specified")
    } else {    
        if (debug) log.debug("sendCmd: '${inputXOR(command)}'")    
	    try {
            if (!interfaces.rawSocket.connected) 
                interfaces.rawSocket.connect("${getDataValue("deviceIP")}", 9999, byteInterface: true)
        
            interfaces.rawSocket.sendMessage(command)
	    } catch (e) {
            if (debug) {
                log.warn("cannot connect to Kasa plug at ${getDataValue("deviceIP")}: ${getStackTrace(e)}")
            } else {
		        log.warn("cannot connect to Kasa plug at ${getDataValue("deviceIP")}")
            }
	    }
    }
}

void socketStatus(message) {
	log.warn("socket status is: ${message}")
}

def parse(message) {
    try {
        if (message && debug) log.debug(parseJson(inputXOR(message)))
    } catch (Exception e) {
        // bad message?
    }
}

private outputXOR(command) {
	String encrCmd = "000000" + Integer.toHexString(command.length())
 	def key = 0xAB
	for (int i = 0; i < command.length(); i++) {
		Integer str = (command.charAt(i) as byte) ^ key
		key = str
		encrCmd += Integer.toHexString(str)
	}
   	return encrCmd
}

private inputXOR(resp) {
	String[] strBytes = resp.substring(8).split("(?<=\\G.{2})")
	String cmdResponse = ""
	byte key = 0xAB
	byte nextKey
	byte[] XORtemp
	for(int i = 0; i < strBytes.length; i++) {
		nextKey = (byte)Integer.parseInt(strBytes[i], 16)	// could be negative
		XORtemp = nextKey ^ key
		key = nextKey
		cmdResponse += new String(XORtemp)
	}
	return cmdResponse
}
