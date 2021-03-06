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
package cc.sferalabs.sfera.drivers.epson_escvp.events;

import cc.sferalabs.sfera.drivers.epson_escvp.EpsonEscVp;
import cc.sferalabs.sfera.events.StringEvent;

/**
 *
 * @author Giampiero Baggiani
 *
 * @version 1.0.0
 *
 */
public class PwrEpsonEscVpEvent extends StringEvent implements EpsonEscVpEvent {

	/**
	 * @param source
	 * @param id
	 * @param value
	 */
	public PwrEpsonEscVpEvent(EpsonEscVp source, String value) {
		super(source, "pwr", getTextValue(value));
	}

	/**
	 * @param value
	 * @return
	 */
	private static String getTextValue(String value) {
		switch (value) {
		case "00":
			return "standby-off";

		case "01":
			return "on";

		case "02":
			return "warmup";

		case "03":
			return "cooling";

		case "04":
			return "standby-on";

		case "05":
			return "standby-fault";

		default:
			return null;
		}
	}

}
