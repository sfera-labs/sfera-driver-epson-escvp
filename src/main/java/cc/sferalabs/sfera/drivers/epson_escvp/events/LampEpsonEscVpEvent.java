/**
 * 
 */
package cc.sferalabs.sfera.drivers.epson_escvp.events;

import cc.sferalabs.sfera.drivers.epson_escvp.EpsonEscVp;
import cc.sferalabs.sfera.events.NumberEvent;

/**
 *
 * @author Giampiero Baggiani
 *
 * @version 1.0.0
 *
 */
public class LampEpsonEscVpEvent extends NumberEvent implements EpsonEscVpEvent {

	/**
	 * @param source
	 * @param id
	 * @param value
	 */
	public LampEpsonEscVpEvent(EpsonEscVp source, String value) {
		super(source, "lamp", Integer.parseInt(value));
	}

}
