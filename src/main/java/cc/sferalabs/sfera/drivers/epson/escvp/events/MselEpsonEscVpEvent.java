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
public class MselEpsonEscVpEvent extends StringEvent implements EpsonEscVpEvent {

	/**
	 * @param source
	 * @param id
	 * @param value
	 */
	public MselEpsonEscVpEvent(EpsonEscVp source, String value) {
		super(source, "msel", getTextValue(value));
	}

	/**
	 * @param value
	 * @return
	 */
	private static String getTextValue(String value) {
		switch (value) {
		case "00":
			return "black";

		case "01":
			return "blue";

		case "02":
			return "logo";

		default:
			return null;
		}
	}

}
