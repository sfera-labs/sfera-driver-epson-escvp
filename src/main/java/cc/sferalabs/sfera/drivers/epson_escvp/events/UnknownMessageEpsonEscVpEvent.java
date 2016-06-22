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
public class UnknownMessageEpsonEscVpEvent extends StringEvent implements EpsonEscVpEvent {

	/**
	 * @param source
	 * @param id
	 * @param value
	 */
	public UnknownMessageEpsonEscVpEvent(EpsonEscVp source, String message) {
		super(source, "unknown", message);
	}

}
