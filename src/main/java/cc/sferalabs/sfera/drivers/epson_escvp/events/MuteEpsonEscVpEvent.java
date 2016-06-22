/**
 * 
 */
package cc.sferalabs.sfera.drivers.epson_escvp.events;

import cc.sferalabs.sfera.drivers.epson_escvp.EpsonEscVp;
import cc.sferalabs.sfera.events.BooleanEvent;

/**
 *
 * @author Giampiero Baggiani
 *
 * @version 1.0.0
 *
 */
public class MuteEpsonEscVpEvent extends BooleanEvent implements EpsonEscVpEvent {

	/**
	 * @param source
	 * @param id
	 * @param value
	 */
	public MuteEpsonEscVpEvent(EpsonEscVp source, String value) {
		super(source, "mute", value.equals("ON"));
	}

}
