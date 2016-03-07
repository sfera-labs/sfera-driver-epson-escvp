/**
 * 
 */
package cc.sferalabs.sfera.drivers.epson.escvp;

import java.nio.charset.StandardCharsets;

/**
 *
 * @author Giampiero Baggiani
 *
 * @version 1.0.0
 *
 */
public class EscVpCommand {

	final String text;
	final byte[] bytes;

	/**
	 * 
	 */
	public EscVpCommand(String cmd) {
		this.text = cmd;
		this.bytes = cmd.getBytes(StandardCharsets.UTF_8);
	}

}
