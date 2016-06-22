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
public class AspectEpsonEscVpEvent extends StringEvent implements EpsonEscVpEvent {

	/**
	 * @param source
	 * @param id
	 * @param value
	 */
	public AspectEpsonEscVpEvent(EpsonEscVp source, String value) {
		super(source, "aspect", getTextValue(value));
	}

	/**
	 * @param value
	 * @return
	 */
	private static String getTextValue(String value) {
		switch (value) {
		case "00":
			return "normal";

		case "10":
			return "4:3";

		case "20":
			return "16:9";

		case "30":
			return "auto";

		case "40":
			return "full";

		case "50":
			return "zoom";

		case "60":
			return "through";

		default:
			return value;
		}
	}

}
