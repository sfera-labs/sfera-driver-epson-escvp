/*-
 * +======================================================================+
 * EpsonEscVp
 * ---
 * Copyright (C) 2016 Sfera Labs S.r.l.
 * ---
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * -======================================================================-
 */

package cc.sferalabs.sfera.drivers.epson_escvp;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import cc.sferalabs.sfera.core.Configuration;
import cc.sferalabs.sfera.drivers.Driver;
import cc.sferalabs.sfera.io.comm.CommPort;
import cc.sferalabs.sfera.io.comm.CommPortException;

public class EpsonEscVp extends Driver {

	private static final long MAX_WRITE_WAIT_TIME_SECONDS = 20;
	private static final long MAX_COMMAND_RESPONSE_TIME_SECONDS = 5;

	private final static byte[] connectCmd = { 0x45, 0x53, 0x43, 0x2F, 0x56, 0x50, 0x2E, 0x6E, 0x65, 0x74, 0x10, 0x03,
			0x00, 0x00, 0x00, 0x00 };
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
	boolean isOn = false;
	long lastPwrUpdate;
	private int errCount = 0;
	final ArrayBlockingQueue<Object> writeLock = new ArrayBlockingQueue<>(1, true);

	public EpsonEscVp(String id) {
		super(id);
	}

	/**
	 * @return
	 */
	Logger getLogger() {
		return log;
	}

	@Override
	protected boolean onInit(Configuration config) throws InterruptedException {
		String addr = config.get("addr", null);
		if (addr == null) {
			log.error("Address not specified in configuration");
			return false;
		}
		pollInterval = config.get("poll_interval", 10) * 1000l;

		try {
			commPort = CommPort.open(addr);
			commPort.setParams(9600, 8, CommPort.STOPBITS_1, CommPort.PARITY_NONE, CommPort.FLOWCONTROL_NONE);
		} catch (CommPortException e) {
			log.error("Error initializing communication", e);
			return false;
		}

		log.debug("Connecting...");
		try {
			boolean net = config.get("net", true);
			if (net) {
				commPort.writeBytes(connectCmd);
				commPort.writeByte(0x0D);
				byte[] connectResp = new byte[16];
				commPort.readBytes(connectResp, 0, connectResp.length, 3000);

				// check type identifier = 3 (CONNECT)
				// and status code = 0x20 (32 - OK)
				if (connectResp[11] != 3 || connectResp[14] != 32) {
					throw new Exception("Connect response error");
				}
			} else {
				commPort.writeBytes(GET_PWR.bytes);
				commPort.writeByte(0x0D);
				byte[] connectResp = new byte[3];
				commPort.readBytes(connectResp, 0, connectResp.length, 3000);
				commPort.clear();
				if (!"PWR".equals(new String(connectResp, StandardCharsets.UTF_8))) {
					throw new Exception("Connect response error");
				}
			}
		} catch (Exception e) {
			log.error("Connection error", e);
			return false;
		}

		try {
			commPort.setListener(new EpsonEscVpCommPortListener(this));
		} catch (CommPortException e) {
			log.error("Error setting comm listener", e);
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
	private synchronized void write(EscVpCommand cmd) throws CommPortException, InterruptedException {
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

	/**
	 * @param cmd
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public synchronized boolean sendCommand(String cmd) throws CommPortException, InterruptedException {
		return writeAndWait(new EscVpCommand(cmd));
	}

	/**
	 * @param code
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean key(String code) throws CommPortException, InterruptedException {
		return sendCommand("KEY " + code);
	}

	/**
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setOn() throws CommPortException, InterruptedException {
		return sendCommand("PWR ON");
	}

	/**
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setOff() throws CommPortException, InterruptedException {
		return sendCommand("PWR OFF");
	}

	/**
	 * @param source
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setSource(String source) throws CommPortException, InterruptedException {
		return sendCommand("SOURCE " + source);
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
		return sendCommand("MSEL " + v);
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
		return sendCommand("ASPECT " + v);
	}

	/**
	 * @param colorMode
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setColorMode(String colorMode) throws CommPortException, InterruptedException {
		return sendCommand("CMODE " + colorMode);
	}

	/**
	 * 
	 * @param highOrLow
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setLuminance(String highOrLow) throws CommPortException, InterruptedException {
		return sendCommand("LUMINANCE " + (highOrLow.equalsIgnoreCase("high") ? "00" : "01"));
	}

	/**
	 * @param mute
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setMute(boolean mute) throws CommPortException, InterruptedException {
		return sendCommand("MUTE " + (mute ? "ON" : "OFF"));
	}

	/**
	 * @param freeze
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setFreeze(boolean freeze) throws CommPortException, InterruptedException {
		return sendCommand("FREEZE " + (freeze ? "ON" : "OFF"));
	}

	/**
	 * @param val
	 * @return
	 * @throws CommPortException
	 * @throws InterruptedException
	 */
	public boolean setAudio(String val) throws CommPortException, InterruptedException {
		return sendCommand("AUDIO " + val);
	}

}
