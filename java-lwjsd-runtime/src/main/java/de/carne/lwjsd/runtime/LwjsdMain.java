/*
 * Copyright (c) 2018 Holger de Carne and contributors, All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.carne.lwjsd.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import de.carne.boot.ApplicationMain;
import de.carne.check.Check;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.runtime.client.Client;
import de.carne.lwjsd.runtime.config.Defaults;
import de.carne.lwjsd.runtime.config.RuntimeConfig;
import de.carne.lwjsd.runtime.logging.SyslogConfig;
import de.carne.lwjsd.runtime.logging.SyslogDestination;
import de.carne.lwjsd.runtime.logging.SyslogHandler;
import de.carne.lwjsd.runtime.logging.SyslogProtocol;
import de.carne.lwjsd.runtime.server.Server;
import de.carne.util.Exceptions;
import de.carne.util.cmdline.CmdLineProcessor;
import de.carne.util.logging.Log;
import de.carne.util.logging.LogBuffer;
import de.carne.util.logging.Logs;

/**
 * Application entry point (for all commands).
 */
public class LwjsdMain implements ApplicationMain {

	private enum Command {

		HELP,

		CLIENT,

		SERVER

	}

	static {
		applyLogConfig(Logs.CONFIG_DEFAULT);
	}

	private static final Log LOG = new Log();

	private static final String NAME = "lwjsd";

	private Command command = Command.HELP;
	private final RuntimeConfig config = new RuntimeConfig(Defaults.get());

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public int run(String[] args) {
		CmdLineProcessor bootCmdLine = buildBootCmdLine(args);
		int status;

		try {
			bootCmdLine.process();

			LOG.info("Running command ''{0}''...", bootCmdLine);

			CmdLineProcessor commandCmdLine = buildCommandCmdLine(args);

			commandCmdLine.process();
			switch (this.command) {
			case HELP:
				status = runHelpCommand();
				break;
			case CLIENT:
				status = runClientCommand();
				break;
			case SERVER:
				status = runServerCommand();
				break;
			default:
				throw Check.fail();
			}
		} catch (Exception e) {
			LOG.error(e, "Command ''{0}'' failed with exception: {1}", bootCmdLine, e.getClass().getName());
			status = -1;
		} finally {
			Logs.flush();
		}
		return status;
	}

	@SuppressWarnings("squid:S106")
	private int runHelpCommand() throws IOException {
		try (BufferedReader helpReader = new BufferedReader(
				new InputStreamReader(getClass().getResourceAsStream("help.txt")))) {
			String helpLine;

			while ((helpLine = helpReader.readLine()) != null) {
				System.out.println(helpLine);
			}
		}
		return 0;
	}

	private int runClientCommand() throws ServiceManagerException {
		try (Client client = new Client(this.config)) {
			client.connect();
			client.requestStop();
		}
		return 0;
	}

	private int runServerCommand() throws InterruptedException, ServiceManagerException {
		SyslogConfig syslogConfig = new SyslogConfig("homefs1.holger.mobi").setProtocol(SyslogProtocol.RFC3164)
				.setDefaultMessageApp(NAME);
		SyslogDestination syslog = new SyslogDestination(syslogConfig);
		SyslogHandler syslogHandler = new SyslogHandler(syslog);

		LogBuffer.addHandler(LOG, syslogHandler, false);
		try (Server server = new Server(this.config)) {
			server.start(true);
		}
		return 0;
	}

	private static void applyLogConfig(String config) {
		try {
			Logs.readConfig(config);
		} catch (IOException e) {
			Exceptions.warn(e);
		}
	}

	private CmdLineProcessor buildBootCmdLine(String[] args) {
		CmdLineProcessor cmdLine = new CmdLineProcessor(name(), args);

		cmdLine.onSwitch(arg -> applyLogConfig(Logs.CONFIG_VERBOSE)).arg("--verbose");
		cmdLine.onSwitch(arg -> applyLogConfig(Logs.CONFIG_DEBUG)).arg("--debug");
		cmdLine.onUnnamedOption(CmdLineProcessor::ignore);
		cmdLine.onUnknownArg(CmdLineProcessor::ignore);
		return cmdLine;
	}

	private CmdLineProcessor buildCommandCmdLine(String[] args) {
		CmdLineProcessor cmdLine = new CmdLineProcessor(name(), args);

		cmdLine.onSwitch(CmdLineProcessor::ignore).arg("--verbose");
		cmdLine.onSwitch(CmdLineProcessor::ignore).arg("--debug");
		cmdLine.onSwitch(this::setMode).arg("--help").arg("--client").arg("--server");
		cmdLine.onOption(this::setBaseUri).arg("--baseUri");
		cmdLine.onUnnamedOption(CmdLineProcessor::ignore);
		cmdLine.onUnknownArg(CmdLineProcessor::ignore);
		return cmdLine;
	}

	private void setMode(String arg) {
		this.command = Command.valueOf(arg.substring(2).toUpperCase());
	}

	private void setBaseUri(String arg, String option) {
		URI baseUri;

		try {
			baseUri = new URI(option);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid option for argument: " + arg, e);
		}
		this.config.setBaseUri(baseUri);
	}

}
