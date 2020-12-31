/*
 * Copyright (c) 2018-2021 Holger de Carne and contributors, All Rights Reserved.
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
package de.carne.lwjsd.runtime.test.logging;

import java.io.IOException;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.UDPNIOTransport;
import org.glassfish.grizzly.nio.transport.UDPNIOTransportBuilder;

import de.carne.lwjsd.api.ServiceContext;
import de.carne.lwjsd.api.ServiceException;
import de.carne.lwjsd.runtime.logging.SyslogConfig;
import de.carne.util.Late;

/**
 * UDP based {@linkplain SyslogReceiver}
 */
public class UdpSyslogReceiverService extends SyslogReceiver {

	/**
	 * UDP listen host
	 */
	public static final String HOST = "localhost";

	/**
	 * UDP listen port
	 */
	public static final int PORT = 1000 + SyslogConfig.DEFAULT_PORT;

	private final Late<UDPNIOTransport> transportHolder = new Late<>();

	@Override
	public void start(ServiceContext context) throws ServiceException {
		UDPNIOTransportBuilder transportBuilder = UDPNIOTransportBuilder.newInstance();

		UDPNIOTransport transport = this.transportHolder.set(transportBuilder.build());

		transport.setProcessor(buildChain(FilterChainBuilder.stateless().add(new TransportFilter())));
		try {
			transport.bind(HOST, PORT);
			transport.start();
		} catch (IOException e) {
			throw new ServiceException(e);
		}
	}

	@Override
	public void stop(ServiceContext context) throws ServiceException {
		try {
			this.transportHolder.get().shutdownNow();
		} catch (IOException e) {
			throw new ServiceException(e);
		}
	}

}
