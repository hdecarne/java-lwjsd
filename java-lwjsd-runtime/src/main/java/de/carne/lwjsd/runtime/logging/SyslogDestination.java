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
package de.carne.lwjsd.runtime.logging;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import de.carne.check.Nullable;
import de.carne.util.Exceptions;
import de.carne.util.logging.Log;

/**
 * An instances of this class represents a single Syslog destination to which {@linkplain SyslogMessage}s can be sent.
 */
public final class SyslogDestination implements Closeable {

	private static final Log LOG = new Log();

	private final SyslogConfig config;
	@Nullable
	private ConnectionHandler connection = null;

	/**
	 * Construct {@linkplain SyslogDestination}.
	 *
	 * @param config The Syslog configuration to use.
	 */
	public SyslogDestination(SyslogConfig config) {
		this.config = config;
	}

	@Override
	public synchronized void close() {
		LOG.info("Closing destination ''{0}''...", this);

		ConnectionHandler checkedConnection = this.connection;

		if (checkedConnection != null) {
			this.connection = null;
			closeConnection(checkedConnection);
		}
	}

	@Override
	public String toString() {
		return this.config.toString();
	}

	/**
	 * Send a {@linkplain SyslogMessage} to this destination.
	 *
	 * @param message The message to send.
	 * @throws IOException if an I/O error occurs while sending the message.
	 */
	public void send(SyslogMessage message) throws IOException {
		IOException error = null;
		int retry = 0;

		do {
			ConnectionHandler checkedConnection = getConnection();

			try {
				checkedConnection.sendMessage(message, this.config);
				error = null;
			} catch (IOException e) {
				if (error == null) {
					error = e;
				} else {
					Exceptions.suppress(e, error);
				}
				retry++;
				closeConnection(checkedConnection);
			}
		} while (error != null && retry < Syslog.RETRY_COUNT);
		if (error != null) {
			throw error;
		}
	}

	private synchronized ConnectionHandler getConnection() throws IOException {
		ConnectionHandler checkedConnection = this.connection;

		if (checkedConnection == null) {
			LOG.info("Opening connection to ''{0}''...", this);
			this.connection = checkedConnection = openConnection();
		} else if (Syslog.CONNECTION_TTL < checkedConnection.ageNanos() || checkedConnection.isStalled()) {
			LOG.info("Re-opening connection to ''{0}''...", this);
			closeConnection(checkedConnection);
			this.connection = checkedConnection = openConnection();
		}
		return checkedConnection;
	}

	private ConnectionHandler openConnection() throws IOException {
		ConnectionHandler checkedConnection;

		if (this.config.hasOption(SyslogOption.TRANSPORT_TCP_TLS)) {
			checkedConnection = new SocketConnectionHandler(this.config.host(), this.config.port(), true);
		} else if (this.config.hasOption(SyslogOption.TRANSPORT_TCP)) {
			checkedConnection = new SocketConnectionHandler(this.config.host(), this.config.port(), false);
		} else {
			checkedConnection = new DatagramSocketConnectionHandler(this.config.host(), this.config.port());
		}
		return checkedConnection;
	}

	private void closeConnection(ConnectionHandler checkedConnection) {
		try {
			checkedConnection.close();
		} catch (IOException e) {
			LOG.warning(e, "An error occurred while closing connection to destination ''{0}''", this);
		}
	}

	private abstract class ConnectionHandler implements Closeable {

		private final long creationTime = System.nanoTime();

		protected ConnectionHandler() {
			// Nothing to do here
		}

		public long ageNanos() {
			return System.nanoTime() - this.creationTime;
		}

		public abstract boolean isStalled();

		public abstract void sendMessage(SyslogMessage message, SyslogConfig messageConfig) throws IOException;

	}

	private class DatagramSocketConnectionHandler extends ConnectionHandler {

		private final InetAddress host;
		private final int port;
		private final DatagramSocket socket;

		public DatagramSocketConnectionHandler(String host, int port) throws IOException {
			this.host = InetAddress.getByName(host);
			this.port = port;
			this.socket = new DatagramSocket();
		}

		@Override
		public void close() {
			this.socket.close();
		}

		@Override
		public boolean isStalled() {
			return this.socket.isClosed();
		}

		@Override
		public void sendMessage(SyslogMessage message, SyslogConfig messageConfig) throws IOException {
			byte[] encodedMessage = message.encode(messageConfig);
			DatagramPacket packet = new DatagramPacket(encodedMessage, encodedMessage.length, this.host, this.port);

			this.socket.send(packet);
		}

	}

	private class SocketConnectionHandler extends ConnectionHandler {

		private final Socket socket;

		SocketConnectionHandler(String host, int port, boolean ssl) throws IOException {
			SocketAddress address = new InetSocketAddress(host, port);

			this.socket = (ssl ? SSLSocketFactory.getDefault() : SocketFactory.getDefault()).createSocket();
			try {
				this.socket.connect(address);
			} catch (IOException e) {
				try {
					this.socket.close();
				} catch (IOException suppressed) {
					Exceptions.suppress(e, suppressed);
				}
				throw e;
			}
		}

		@Override
		public void close() throws IOException {
			this.socket.close();
		}

		@Override
		public boolean isStalled() {
			return !this.socket.isConnected() || this.socket.isClosed() || this.socket.isOutputShutdown();
		}

		@Override
		public void sendMessage(SyslogMessage message, SyslogConfig messageConfig) throws IOException {
			OutputStream out = this.socket.getOutputStream();

			message.encodeTo(out, messageConfig);
			out.flush();
		}

	}

}
