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
package de.carne.lwjsd.runtime.test.logging;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

import de.carne.check.Nullable;
import de.carne.lwjsd.runtime.logging.Syslog;
import de.carne.lwjsd.runtime.logging.SyslogConfig;
import de.carne.lwjsd.runtime.logging.SyslogOption;
import de.carne.util.Exceptions;
import de.carne.util.logging.Log;

/**
 * Helper class used to collect and validate send Syslog messages.
 */
class SyslogMessageReceiver implements AutoCloseable {

	private static final Log LOG = new Log();

	private static final int TIMEOUT = 500;

	private final ExecutorService threads = Executors.newSingleThreadExecutor();
	private final SyslogConfig config;
	private final Future<SyslogMessageReceiver> listener;
	private boolean ready = false;
	private final LinkedBlockingDeque<byte[]> messages = new LinkedBlockingDeque<>();

	SyslogMessageReceiver(SyslogConfig config) throws InterruptedException {
		this.config = config;

		Set<SyslogOption> options = this.config.getOptions();

		if (options.contains(SyslogOption.TRANSPORT_TCP_TLS)) {
			this.listener = this.threads.submit(this::serverSocketListener, this);
		} else if (options.contains(SyslogOption.TRANSPORT_TCP)) {
			this.listener = this.threads.submit(this::serverSocketListener, this);
		} else {
			this.listener = this.threads.submit(this::datagramSocketListener, this);
		}
		waitReady();
	}

	@Nullable
	public byte[] pollMessage() throws InterruptedException {
		return this.messages.poll(2 * TIMEOUT, TimeUnit.MILLISECONDS);
	}

	@Override
	public void close() throws InterruptedException {
		this.listener.cancel(true);
		this.threads.shutdown();
		this.threads.awaitTermination(2 * TIMEOUT, TimeUnit.MICROSECONDS);
	}

	private void waitReady() throws InterruptedException {
		boolean wait = true;

		while (wait) {
			synchronized (this) {
				this.wait();
				wait = !this.ready;
			}
		}
	}

	private synchronized void signalReady() {
		this.ready = true;
		notify();
	}

	private void datagramSocketListener() {
		byte[] receiveBuffer = new byte[1024];

		try (DatagramSocket socket = new DatagramSocket(
				new InetSocketAddress(this.config.host(), this.config.port()))) {
			socket.setSoTimeout(TIMEOUT);
			signalReady();

			while (!this.listener.isCancelled()) {
				LOG.info("Waiting for UDP message...");

				try {
					DatagramPacket received = new DatagramPacket(receiveBuffer, receiveBuffer.length);

					socket.receive(received);

					LOG.info("Message from ''{0}''", received.getAddress());
					LOG.info("{0} byte(s) received", received.getLength());

					byte[] message = new byte[received.getLength()];

					System.arraycopy(received.getData(), received.getOffset(), message, 0, message.length);
					this.messages.add(message);
				} catch (SocketTimeoutException e) {
					Exceptions.ignore(e);
				}
			}
			LOG.info("Shutting down UDP listener...");
		} catch (IOException e) {
			Exceptions.warn(e);
		}
	}

	private void serverSocketListener() {
		SocketAddress address = new InetSocketAddress(this.config.host(), this.config.port());
		ServerSocketFactory factory = (this.config.hasOption(SyslogOption.TRANSPORT_TCP_TLS)
				? SSLServerSocketFactory.getDefault()
				: ServerSocketFactory.getDefault());

		try (ServerSocket serverSocket = factory.createServerSocket()) {
			serverSocket.setSoTimeout(TIMEOUT);
			serverSocket.bind(address);
			signalReady();
			while (!this.listener.isCancelled()) {
				LOG.info("Waiting for TCP connection...");

				try (Socket clientSocket = serverSocket.accept()) {
					byte[] receiveBuffer = new byte[1024];
					int received = 0;

					while (true) {
						LOG.info("Receiving message from client ''{0}''", clientSocket.getInetAddress());

						int read = clientSocket.getInputStream().read(receiveBuffer, received,
								receiveBuffer.length - received);

						LOG.info("{0} byte(s) received", read);

						if (read < 0 || !clientSocket.isConnected() || clientSocket.isInputShutdown()) {
							LOG.warning("Client ''{0}'' gone", clientSocket.getInetAddress());
							break;
						}
						received += read;
						if (isMessageComplete(receiveBuffer, received)) {
							byte[] message = new byte[received];

							System.arraycopy(receiveBuffer, 0, message, 0, message.length);
							this.messages.add(message);
							received = 0;
						}
					}
				} catch (SocketTimeoutException e) {
					Exceptions.ignore(e);
				}
			}
			LOG.info("Shutting down TCP listener...");
		} catch (IOException e) {
			Exceptions.warn(e);
		}
	}

	private boolean isMessageComplete(byte[] receiveBuffer, int received) {
		boolean complete = false;

		if (received > 0) {
			if (receiveBuffer[0] != '<') {
				int messageSize = 0;
				int messageIndex = 0;

				while (messageIndex < received) {
					byte messageByte = receiveBuffer[messageIndex];

					if ('0' <= messageByte && messageByte <= '9') {
						messageSize = (messageSize * 10) + (messageByte - '0');
					} else if (messageByte == ' ') {
						break;
					} else {
						messageSize = Integer.MAX_VALUE;
						break;
					}
					messageIndex++;
				}
				complete = received == messageIndex + messageSize;
			} else {
				byte[] trailerBytes = Syslog.NON_TRANSPARENT_FRAMING_TRAILER.getBytes(StandardCharsets.US_ASCII);

				complete = Arrays.compare(receiveBuffer, Math.max(received - trailerBytes.length, 0), received,
						trailerBytes, 0, trailerBytes.length) == 0;
			}
		}
		return complete;
	}

}
