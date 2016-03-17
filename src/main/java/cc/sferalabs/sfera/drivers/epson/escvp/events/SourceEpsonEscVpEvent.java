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
public class SourceEpsonEscVpEvent extends StringEvent implements EpsonEscVpEvent {

	/**
	 * @param source
	 * @param id
	 * @param value
	 */
	public SourceEpsonEscVpEvent(EpsonEscVp source, String value) {
		super(source, "source", value);
	}

}