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
public class LuminanceEpsonEscVpEvent extends StringEvent implements EpsonEscVpEvent {

	/**
	 * @param source
	 * @param id
	 * @param value
	 */
	public LuminanceEpsonEscVpEvent(EpsonEscVp source, String value) {
		super(source, "luminance", getTextValue(value));
	}

	/**
	 * @param value
	 * @return
	 */
	private static String getTextValue(String value) {
		switch (value) {
		case "00":
			return "high";

		case "01":
			return "low";

		default:
			return null;
		}
	}

}
