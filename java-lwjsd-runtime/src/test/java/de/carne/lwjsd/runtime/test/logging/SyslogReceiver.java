/*
 * Copyright (c) 2018-2019 Holger de Carne and contributors, All Rights Reserved.
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
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.nio.transport.UDPNIOConnection;

import de.carne.boot.Exceptions;
import de.carne.boot.check.Check;
import de.carne.lwjsd.api.Service;
import de.carne.lwjsd.runtime.logging.SyslogConfig;

abstract class SyslogReceiver implements Service {

	private static final int TIMEOUT = 5000;

	private static final Pattern RFC3164_PATTERN = Pattern
			.compile("^<\\d+>\\w{3} \\d{2} \\d{2}:\\d{2}:\\d{2} \\S+ \\S+ (.+)$");
	private static final Pattern RFC5424_PATTERN = Pattern
			.compile("^<\\d+>1 \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d+Z \\S+ \\S+ \\S+ \\S+ \\S+ .{3}(.+)$");

	private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

	@Nullable
	public String pollMessage(SyslogConfig config) throws InterruptedException {
		@Nullable String message = this.messages.poll(TIMEOUT, TimeUnit.MILLISECONDS);

		if (message == null) {
			throw new IllegalStateException("Unexpected " + config.getProtocol() + " poll timeout");
		}

		Matcher matcher;

		switch (config.getProtocol()) {
		case RFC3164:
			matcher = RFC3164_PATTERN.matcher(message);
			break;
		case RFC5424:
			matcher = RFC5424_PATTERN.matcher(message);
			break;
		default:
			throw Check.unexpected(config.getProtocol());
		}
		if (!matcher.matches()) {
			throw new IllegalStateException("Unexpected " + config.getProtocol() + " message: " + message);
		}
		return matcher.group(1);
	}

	protected FilterChain buildChain(FilterChainBuilder chainBuilder) {
		chainBuilder.add(new SyslogFilter(this::handleRead));
		return chainBuilder.build();
	}

	private NextAction handleRead(FilterChainContext ctx) {
		return (ctx.getConnection() instanceof UDPNIOConnection ? handleUdpRead(ctx) : handleTcpRead(ctx));
	}

	private NextAction handleUdpRead(FilterChainContext ctx) {
		Buffer buffer = ctx.getMessage();
		String message = buffer.toStringContent(StandardCharsets.US_ASCII);

		this.messages.offer(message);
		return ctx.getInvokeAction();
	}

	private NextAction handleTcpRead(FilterChainContext ctx) {
		Buffer buffer = ctx.getMessage();
		String message = buffer.toStringContent(StandardCharsets.US_ASCII);
		int startIndex = message.indexOf('<');
		NextAction nextAction = ctx.getStopAction();

		if (startIndex == 0) {
			if ("\n\0".indexOf(message.charAt(message.length() - 1)) >= 0) {
				this.messages.offer(message.substring(0, message.length() - 1));
				nextAction = ctx.getInvokeAction();
			}
		} else if (startIndex > 1 && message.charAt(startIndex - 1) == ' ') {
			int messageLength;

			try {
				messageLength = Integer.parseInt(message.substring(0, startIndex - 1));
				if (startIndex + messageLength == buffer.limit()) {
					this.messages.offer(message.substring(startIndex));
					nextAction = ctx.getInvokeAction();
				}
			} catch (NumberFormatException e) {
				Exceptions.ignore(e);
				nextAction = ctx.getInvokeAction();
			}
		}
		return nextAction;
	}

	private class SyslogFilter extends BaseFilter {

		private final Function<FilterChainContext, NextAction> handler;

		public SyslogFilter(Function<FilterChainContext, NextAction> handler) {
			this.handler = handler;
		}

		@Override
		public NextAction handleRead(@Nullable FilterChainContext ctx) throws IOException {
			return this.handler.apply(Objects.requireNonNull(ctx));
		}

	}

}
