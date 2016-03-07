package cc.sferalabs.sfera.drivers.epson.escvp;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import cc.sferalabs.sfera.core.Configuration;
import cc.sferalabs.sfera.drivers.Driver;
import cc.sferalabs.sfera.drivers.epson.escvp.events.AspectEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson.escvp.events.AudioEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson.escvp.events.ColorModeEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson.escvp.events.EpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson.escvp.events.ErrorEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson.escvp.events.FreezeEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson.escvp.events.LampEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson.escvp.events.LuminanceEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson.escvp.events.MselEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson.escvp.events.MuteEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson.escvp.events.PwrEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson.escvp.events.SourceEpsonEscVpEvent;
import cc.sferalabs.sfera.drivers.epson.escvp.events.UnknownMessageEpsonEscVpEvent;
import cc.sferalabs.sfera.events.Bus;
import cc.sferalabs.sfera.io.comm.CommPort;
import cc.sferalabs.sfera.io.comm.CommPortException;
import cc.sferalabs.sfera.io.comm.CommPortListener;

public class EpsonEscVp extends Driver implements CommPortListener {

	private static final String PARAM_ADDR = "addr";
	private static final String PARAM_POLL_INTERVAL = "poll_interval";

	private static final long MAX_WRITE_WAIT_TIME_SECONDS = 20;
	private static final long MAX_COMMAND_RESPONSE_TIME_SECONDS = 5;

	private static final EscVpCommand GET_PWR = new EscVpCommand("PWR?");
	private static final EscVpCommand GET_ERR = new EscVpCommand("ERR?");
	private static final EscVpCommand GET_SOURCE = new EscVpCommand("SOURCE?");
	private static final EscVpCommand GET_MSEL = new EscVpCommand("MSEL?");
	private static final EscVpCommand GET_ASPECT = new EscVpCommand("ASPECT?");
	private static final EscVpCommand GET_CMODE = new EscVpCommand("CMODE?");
	private static final EscVpCommand GET_LAMP = new EscVpCommand("LAMP?");
	private static final EscVpCommand GET_MUTE = new EscVpCommand("MUTE?");
	// private static final EscVpCommand GET_FREEZE = new
	// EscVpCommand("FREEZE?");
	// private static final EscVpCommand GET_AUDIO = new EscVpCommand("AUDIO?");

	private long pollInterval;

	private CommPort commPort;
	private StringBuilder message = new StringBuilder();
	private boolean isOn = false;
	private int errCount = 0;
	private final ArrayBlockingQueue<Object> writeLock = new ArrayBlockingQueue<>(1, true);
	private long lastPwrUpdate;

	public EpsonEscVp(String id) {
		super(id);
	}

	@Override
	protected boolean onInit(Configuration config) throws InterruptedException {
		String addr = config.get(PARAM_ADDR, null);
		if (addr == null) {
			log.error("Param {} not specified in configuration", PARAM_ADDR);
			return false;
		}
		pollInterval = config.get(PARAM_POLL_INTERVAL, 10) * 1000l;

		try {
			commPort = CommPort.open(addr);
			commPort.setParams(9600, 8, CommPort.STOPBITS_1, CommPort.PARITY_NONE,
					CommPort.FLOWCONTROL_NONE);
			commPort.setListener(this);
		} catch (CommPortException e) {
			log.error("Error initializing communication", e);
			return false;
		}

		writeLock.offer(this);
		lastPwrUpdate = System.currentTimeMillis();
		return true;
	}

	@Override
	protected boolean loop() throws InterruptedException {
		try {
			if (lastPwrUpdate < System.currentTimeMillis() - (pollInterval * 10)) {
				log.error("No state updates in a while");
				return false;
			}
			writeAndWait(GET_PWR);
			if (isOn) {
				write(GET_ERR);
				write(GET_SOURCE);
				write(GET_MSEL);
				write(GET_ASPECT);
				write(GET_CMODE);
				write(GET_LAMP);
				write(GET_MUTE);
				// write(GET_FREEZE);
				// write(GET_AUDIO);
			}
			errCount = 0;
			Thread.sleep(pollInterval);
		} catch (CommPortException e) {
			log.debug("Write error", e);
			if (++errCount > 4) {
				log.error("Too many write errors");
				return false;
			}
		}

		return true;
	}

	/**
	 * 
	 * @param cmd
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	private synchronized void write(EscVpCommand cmd)
			throws CommPortException, InterruptedException {
		if (writeLock.poll(MAX_WRITE_WAIT_TIME_SECONDS, TimeUnit.SECONDS) == null) {
			log.debug("Write lock timeout elapsed {}", cmd.text);
		}
		log.debug("Writing: {}", cmd.text);
		commPort.writeBytes(cmd.bytes);
		commPort.writeByte(0x0D);
	}

	/**
	 * 
	 * @param cmd
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	private boolean writeAndWait(EscVpCommand cmd) throws CommPortException, InterruptedException {
		try {
			write(cmd);
			return writeLock.poll(MAX_COMMAND_RESPONSE_TIME_SECONDS, TimeUnit.SECONDS) != null;
		} finally {
			writeLock.offer(this);
		}
	}

	@Override
	protected void onQuit() {
		if (commPort != null) {
			try {
				commPort.close();
			} catch (CommPortException e) {
				log.warn("Error closing comm port");
			}
		}
	}

	@Override
	public void onRead(byte[] bytes) {
		for (byte b : bytes) {
			if (b == (byte) ':') {
				writeLock.offer(this);
			} else if (b == (byte) 0x0D) {
				processMessage(message.toString());
				message = new StringBuilder();
			} else {
				char c = (char) (b & 0xFF);
				message.append(c);
			}
		}
	}

	@Override
	public void onError(Throwable t) {
		log.warn("Communication error", t);
	}

	/**
	 * 
	 * @param message
	 */
	private void processMessage(String message) {
		log.debug("Processing message: {}", message);
		int eqIdx = message.indexOf("=");
		if (eqIdx > 0) {
			String command = message.substring(0, eqIdx);
			String param = message.substring(eqIdx + 1);
			EpsonEscVpEvent e;
			switch (command) {
			case "PWR":
				e = new PwrEpsonEscVpEvent(this, param);
				isOn = "01".equals(param);
				lastPwrUpdate = System.currentTimeMillis();
				break;

			case "ERR":
				e = new ErrorEpsonEscVpEvent(this, param);
				break;

			case "SOURCE":
				e = new SourceEpsonEscVpEvent(this, param);
				break;

			case "MSEL":
				e = new MselEpsonEscVpEvent(this, param);
				break;

			case "ASPECT":
				e = new AspectEpsonEscVpEvent(this, param);
				break;

			case "CMODE":
				e = new ColorModeEpsonEscVpEvent(this, param);
				break;

			case "LAMP":
				e = new LampEpsonEscVpEvent(this, param);
				break;

			case "LUMINANCE":
				e = new LuminanceEpsonEscVpEvent(this, param);
				break;

			case "MUTE":
				e = new MuteEpsonEscVpEvent(this, param);
				break;

			case "FREEZE":
				e = new FreezeEpsonEscVpEvent(this, param);
				break;

			case "AUDIO":
				e = new AudioEpsonEscVpEvent(this, param);
				break;

			default:
				e = null;
				break;
			}

			if (e != null) {
				Bus.postIfChanged(e);
				return;
			}
		}

		if (message.equals("ERR")) {
			log.warn("Received command error (ERR) response");
			return;
		}

		Bus.postIfChanged(new UnknownMessageEpsonEscVpEvent(this, message));
	}

	/**
	 * @param cmd
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public synchronized boolean sendSetCommand(String cmd)
			throws CommPortException, InterruptedException {
		return writeAndWait(new EscVpCommand(cmd));
	}

	/**
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setOn() throws CommPortException, InterruptedException {
		return sendSetCommand("PWR ON");
	}

	/**
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setOff() throws CommPortException, InterruptedException {
		return sendSetCommand("PWR OFF");
	}

	/**
	 * @param source
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setSource(String source) throws CommPortException, InterruptedException {
		return sendSetCommand("SOURCE " + source);
	}

	/**
	 * @param val
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setMsel(String val) throws CommPortException, InterruptedException {
		String v;
		if (val.equalsIgnoreCase("black") || val.endsWith("0")) {
			v = "00";
		} else if (val.equalsIgnoreCase("blue") || val.endsWith("1")) {
			v = "01";
		} else if (val.equalsIgnoreCase("logo") || val.endsWith("2")) {
			v = "02";
		} else {
			v = val;
		}
		return sendSetCommand("MSEL " + v);
	}

	/**
	 * @param val
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setAspect(String val) throws CommPortException, InterruptedException {
		String v;
		if (val.equalsIgnoreCase("normal")) {
			v = "00";
		} else if (val.equals("4:3")) {
			v = "10";
		} else if (val.equals("16:9")) {
			v = "20";
		} else if (val.equalsIgnoreCase("auto")) {
			v = "30";
		} else if (val.equalsIgnoreCase("full")) {
			v = "40";
		} else if (val.equalsIgnoreCase("zoom")) {
			v = "50";
		} else if (val.equalsIgnoreCase("through")) {
			v = "60";
		} else {
			v = val;
		}
		return sendSetCommand("ASPECT " + v);
	}

	/**
	 * @param colorMode
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setColorMode(String colorMode) throws CommPortException, InterruptedException {
		return sendSetCommand("CMODE " + colorMode);
	}

	/**
	 * 
	 * @param highOrLow
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setLuminance(String highOrLow) throws CommPortException, InterruptedException {
		return sendSetCommand("LUMINANCE " + (highOrLow.equalsIgnoreCase("high") ? "00" : "01"));
	}

	/**
	 * @param mute
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setMute(boolean mute) throws CommPortException, InterruptedException {
		return sendSetCommand("MUTE " + (mute ? "ON" : "OFF"));
	}

	/**
	 * @param freeze
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setFreeze(boolean freeze) throws CommPortException, InterruptedException {
		return sendSetCommand("FREEZE " + (freeze ? "ON" : "OFF"));
	}

	/**
	 * @param val
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setAudio(String val) throws CommPortException, InterruptedException {
		return sendSetCommand("AUDIO " + val);
	}

}
