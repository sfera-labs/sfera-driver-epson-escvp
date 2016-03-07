/**
 * 
 */
package cc.sferalabs.sfera.drivers.epson.escvp.events;

import cc.sferalabs.sfera.drivers.epson.escvp.EpsonEscVp;
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
			return "wormup";

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
