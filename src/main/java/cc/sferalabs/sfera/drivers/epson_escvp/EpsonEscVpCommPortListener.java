/*-
 * +======================================================================+
 * EpsonEscVp
 * ---
 * Copyright (C) 2016 Sfera Labs S.r.l.
 * ---
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * -======================================================================-
 */

/**
 * 
 */
package cc.sferalabs.sfera.drivers.epson_escvp;

import cc.sferalabs.sfera.drivers.epson_escvp.events.AspectEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson_escvp.events.AudioEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson_escvp.events.ColorModeEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson_escvp.events.EpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson_escvp.events.ErrorEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson_escvp.events.FreezeEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson_escvp.events.LampEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson_escvp.events.LuminanceEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson_escvp.events.MselEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson_escvp.events.MuteEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson_escvp.events.PwrEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson_escvp.events.SourceEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson_escvp.events.UnknownMessageEpsonEscVpEvent;
import cc.sferalabs.sfera.events.Bus;
import cc.sferalabs.sfera.io.comm.CommPortListener;

/**
 *
 * @author Giampiero Baggiani
 *
 * @version 1.0.0
 *
 */
public class EpsonEscVpCommPortListener implements CommPortListener {

	private final EpsonEscVp driver;
	private StringBuilder message = new StringBuilder();;

	/**
	 * 
	 */
	EpsonEscVpCommPortListener(EpsonEscVp driver) {
		this.driver = driver;
	}

	@Override
	public void onRead(byte[] bytes) {
		for (byte b : bytes) {
			if (b == (byte) ':') {
				driver.writeLock.offer(this);
			} else if (b == (byte) 0x0D) {
				processMessage(message.toString());
				message = new StringBuilder();
			} else {
				char c = (char) (b & 0xFF);
				message.append(c);
			}
		}
	}

	@Override
	public void onError(Throwable t) {
		driver.getLogger().warn("Communication error", t);
	}

	/**
	 * 
	 * @param message
	 */
	private void processMessage(String message) {
		driver.getLogger().debug("Processing message: {}", message);
		if (message.equals("PWR?")) {
			// Echo from projector happening in some cases when it is off
			message = "PWR=00";
		}
		int eqIdx = message.indexOf("=");
		if (eqIdx > 0) {
			String command = message.substring(0, eqIdx);
			String param = message.substring(eqIdx + 1);
			EpsonEscVpEvent e;
			switch (command) {
			case "PWR":
				e = new PwrEpsonEscVpEvent(driver, param);
				driver.isOn = "01".equals(param);
				driver.lastPwrUpdate = System.currentTimeMillis();
				break;

			case "ERR":
				e = new ErrorEpsonEscVpEvent(driver, param);
				break;

			case "SOURCE":
				e = new SourceEpsonEscVpEvent(driver, param);
				break;

			case "MSEL":
				e = new MselEpsonEscVpEvent(driver, param);
				break;

			case "ASPECT":
				e = new AspectEpsonEscVpEvent(driver, param);
				break;

			case "CMODE":
				e = new ColorModeEpsonEscVpEvent(driver, param);
				break;

			case "LAMP":
				e = new LampEpsonEscVpEvent(driver, param);
				break;

			case "LUMINANCE":
				e = new LuminanceEpsonEscVpEvent(driver, param);
				break;

			case "MUTE":
				e = new MuteEpsonEscVpEvent(driver, param);
				break;

			case "FREEZE":
				e = new FreezeEpsonEscVpEvent(driver, param);
				break;

			case "AUDIO":
				e = new AudioEpsonEscVpEvent(driver, param);
				break;

			default:
				e = null;
				break;
			}

			if (e != null) {
				Bus.postIfChanged(e);
				return;
			}
		}

		if (message.equals("ERR")) {
			driver.getLogger().warn("Received command error (ERR) response");
			return;
		}

		Bus.post(new UnknownMessageEpsonEscVpEvent(driver, message));
	}

}
