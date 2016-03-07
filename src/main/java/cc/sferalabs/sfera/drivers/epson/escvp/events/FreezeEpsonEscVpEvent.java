/**
 * 
 */
package cc.sferalabs.sfera.drivers.epson.escvp.events;

import cc.sferalabs.sfera.drivers.epson.escvp.EpsonEscVp;
import cc.sferalabs.sfera.events.BooleanEvent;

/**
 *
 * @author Giampiero Baggiani
 *
 * @version 1.0.0
 *
 */
public class FreezeEpsonEscVpEvent extends BooleanEvent implements EpsonEscVpEvent {

	/**
	 * @param source
	 * @param id
	 * @param value
	 */
	public FreezeEpsonEscVpEvent(EpsonEscVp source, String value) {
		super(source, "freeze", value.equals("ON"));
	}

}
