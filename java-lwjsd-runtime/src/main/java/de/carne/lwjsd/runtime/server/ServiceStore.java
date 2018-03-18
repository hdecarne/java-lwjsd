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
package de.carne.lwjsd.runtime.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import de.carne.check.Check;
import de.carne.check.Nullable;
import de.carne.lwjsd.api.Service;
import de.carne.lwjsd.api.ServiceException;
import de.carne.lwjsd.api.ServiceManager;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceState;
import de.carne.lwjsd.runtime.config.Config;
import de.carne.lwjsd.runtime.config.SecretsStore;
import de.carne.nio.file.attribute.FileAttributes;
import de.carne.util.Strings;
import de.carne.util.logging.Log;

/**
 * This class provides the low level module and service management functions required for server execution.
 */
public final class ServiceStore {

	private static final Log LOG = new Log();

	private static final String STATE_FILE = "lwjsd.state.xml";
	private static final String MODULES_DIR = "modules";
	private static final String SERVICE_MODULE_NAME_PATTERN = "service%1$d.moduleName";
	private static final String SERVICE_CLASS_NAME_PATTERN = "service%1$d.className";
	private static final String SERVICE_START_FLAG_PATTERN = "service%1$d.startFlag";

	/**
	 * Information attributes of a registered service.
	 */
	public abstract class Entry {

		private final Optional<String> moduleName;
		private final String className;
		private final boolean startFlag;

		/**
		 * Constructs a new {@linkplain Entry} instance.
		 *
		 * @param moduleName the (optional) service module name.
		 * @param className the service class name.
		 * @param startFlag the service start flag.
		 */
		protected Entry(Optional<String> moduleName, String className, boolean startFlag) {
			this.moduleName = moduleName;
			this.className = className;
			this.startFlag = startFlag;
		}

		/**
		 * Gets the (optional) service module name.
		 *
		 * @return the (optional) service module name.
		 */
		public Optional<String> moduleName() {
			return this.moduleName;
		}

		/**
		 * Gets the service class name.
		 *
		 * @return the service class name.
		 */
		public String className() {
			return this.className;
		}

		/**
		 * Gets the service start flag.
		 *
		 * @return the service start flag.
		 */
		public boolean getStartFlag() {
			return this.startFlag;
		}

		/**
		 * Gets the current service state.
		 *
		 * @return the current service state.
		 * @see ServiceState
		 */
		public abstract ServiceState getState();

		@Override
		public String toString() {
			return formatServiceName(this.moduleName, this.className);
		}

	}

	/**
	 * Formats a service name.
	 *
	 * @param moduleName the (optional) service module.
	 * @param className the service class name.
	 * @return the formatted service name.
	 */
	public static String formatServiceName(Optional<String> moduleName, String className) {
		StringBuilder buffer = new StringBuilder(className);

		moduleName.ifPresent(name -> buffer.append('@').append(name));
		return buffer.toString();
	}

	private class RuntimeEntry extends Entry {

		private ServiceState state = ServiceState.REGISTERED;
		@Nullable
		private Service service = null;

		public RuntimeEntry(Optional<String> moduleName, String className, boolean startFlag) {
			super(moduleName, className, startFlag);
		}

		@Override
		public ServiceState getState() {
			return this.state;
		}

		public Service getService() {
			return Check.notNull(this.service);
		}

		public void updateState(ServiceState state, @Nullable Service service) {
			this.state = state;
			this.service = service;
		}

	}

	private final Path modulesDir;
	private final Path stateFile;
	private final Map<String, RuntimeEntry> stateMap = new HashMap<>();
	private final Map<String, ClassLoader> moduleMap = new HashMap<>();

	private ServiceStore(Path modulesDir, Path stateFile) {
		this.modulesDir = modulesDir;
		this.stateFile = stateFile;
	}

	/**
	 * Creates a new {@linkplain ServiceStore} instance by loading or (if not yet existing) creating the necessary state
	 * data.
	 *
	 * @param config the {@linkplain Config} object to use for store initialization.
	 * @return the created {@linkplain ServiceStore} instance.
	 * @throws IOException if an I/O error occurs while accessing the store data.
	 */
	public static ServiceStore open(Config config) throws IOException {
		Path stateDir = config.getStateDir();

		Files.createDirectories(stateDir, FileAttributes.userDirectoryDefault(stateDir));

		Path modulesDir = stateDir.resolve(stateDir);

		LOG.info("Using modules directory ''{0}''...", modulesDir);

		Files.createDirectories(modulesDir, FileAttributes.userDirectoryDefault(stateDir));

		Path stateFile = stateDir.resolve(STATE_FILE);

		LOG.info("Using state file ''{0}''...", stateFile);

		Properties state = new Properties();
		boolean syncStore = false;

		if (Files.exists(stateFile)) {
			try (InputStream stateStream = Files.newInputStream(stateFile, StandardOpenOption.READ)) {
				state.loadFromXML(stateStream);
			}
		} else {
			syncStore = true;
		}

		ServiceStore serviceStore = new ServiceStore(modulesDir, stateFile);
		Integer serviceIndex = Integer.valueOf(0);

		while (true) {
			String moduleNameKey = String.format(SERVICE_MODULE_NAME_PATTERN, serviceIndex);
			String moduleNameValue = state.getProperty(moduleNameKey);
			String classNameKey = String.format(SERVICE_CLASS_NAME_PATTERN, serviceIndex);
			String classNameValue = state.getProperty(classNameKey);
			String startFlagKey = String.format(SERVICE_START_FLAG_PATTERN, serviceIndex);
			String startFlagValue = state.getProperty(startFlagKey);

			if (moduleNameValue == null && classNameValue == null && startFlagValue == null) {
				break;
			}

			String moduleName = Strings.trim(moduleNameValue);
			String className = Strings.safeTrim(classNameValue);
			boolean startFlag = Boolean.parseBoolean(Strings.safeTrim(startFlagValue));

			if (moduleNameValue != null && Strings.isEmpty(classNameValue)) {
				LOG.warning("Ignoring service entry with invalid module name (''{0}'' = ''{1}'')", moduleNameKey,
						moduleNameValue);
			} else if (Strings.isEmpty(classNameValue)) {
				LOG.warning("Ignoring service entry with invalid class name (''{0}'' = ''{1}'')", moduleNameKey,
						moduleNameValue);
			} else {
				serviceStore.registerService(Optional.ofNullable(moduleName), className, startFlag);
			}
			serviceIndex = Integer.valueOf(serviceIndex.intValue() + 1);
		}
		if (syncStore) {
			serviceStore.syncStore();
		}
		return serviceStore;
	}

	private synchronized void syncStore() throws IOException {
		Properties state = new Properties();
		Integer serviceIndex = Integer.valueOf(0);

		for (RuntimeEntry entry : this.stateMap.values()) {
			String moduleNameKey = String.format(SERVICE_MODULE_NAME_PATTERN, serviceIndex);
			String classNameKey = String.format(SERVICE_CLASS_NAME_PATTERN, serviceIndex);
			String startFlagKey = String.format(SERVICE_START_FLAG_PATTERN, serviceIndex);

			entry.moduleName().ifPresent(name -> state.setProperty(moduleNameKey, name));
			state.setProperty(classNameKey, entry.className());
			state.setProperty(startFlagKey, Boolean.toString(entry.getStartFlag()));
			serviceIndex = Integer.valueOf(serviceIndex.intValue() + 1);
		}
		try (OutputStream stateStream = Files.newOutputStream(this.stateFile, StandardOpenOption.WRITE,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			state.storeToXML(stateStream, null);
		}
		LOG.notice("Created/updated state has been written to file ''{0}''...", this.stateFile);
	}

	/**
	 * Registers a new service entry into the store.
	 *
	 * @param moduleName the (optional) service module name.
	 * @param className the service class name.
	 * @param startFlag the service start flag.
	 * @throws IOException if an I/O error occurs while updating the store data.
	 */
	public synchronized void registerService(Optional<String> moduleName, String className, boolean startFlag)
			throws IOException {
		RuntimeEntry entry = new RuntimeEntry(moduleName, className, startFlag);

		if (!this.stateMap.containsKey(className)) {
			this.stateMap.put(entry.className(), entry);
			syncStore();
			LOG.notice("Registered new service ''{0}''", entry);
		} else {
			LOG.debug("Ignoring register request for already registered service ''{0}''", entry);
		}
	}

	/**
	 * Queries all currently registered service entries.
	 *
	 * @return Collection of all currently registered service entries.
	 */
	public synchronized Collection<Entry> queryEntries() {
		return new ArrayList<>(this.stateMap.values());
	}

	public synchronized void startService(String className, ServiceManager serviceManager, SecretsStore secretsStore)
			throws ServiceManagerException {
		RuntimeEntry serviceEntry = this.stateMap.get(className);

		if (serviceEntry == null) {
			throw new ServiceManagerException("Failed to start unknown service ''{0}''", className);
		}

		if (serviceEntry.getState() == ServiceState.REGISTERED) {
			LOG.info("Loading service ''{0}''", serviceEntry);

			ClassLoader serviceLoader = getServiceLoader(serviceEntry.moduleName(), secretsStore);
			Service service;

			try {
				service = serviceLoader.loadClass(className).asSubclass(Service.class).getConstructor().newInstance();
				service.load(serviceManager);
			} catch (ReflectiveOperationException | ServiceException e) {
				throw new ServiceManagerException(e, "Failed to load server ''{0}''", serviceEntry);
			}
			serviceEntry.updateState(ServiceState.LOADED, service);

			LOG.notice("Loaded service ''{0}''", serviceEntry);
		}
		if (serviceEntry.getState() == ServiceState.LOADED) {
			LOG.info("Starting service ''{0}''", serviceEntry);

			Service service = serviceEntry.getService();

			try {
				service.start(serviceManager);
			} catch (ServiceException e) {
				throw new ServiceManagerException(e, "Failed to start server ''{0}''", serviceEntry);
			}
			serviceEntry.updateState(ServiceState.RUNNING, service);

			LOG.notice("Started service ''{0}''", serviceEntry);
		}
	}

	private ClassLoader getServiceLoader(Optional<String> optionalModuleName, SecretsStore secretsStore)
			throws ServiceManagerException {
		ClassLoader serviceLoader;

		if (optionalModuleName.isPresent()) {
			String moduleName = optionalModuleName.get();

			serviceLoader = this.moduleMap.get(moduleName);
			if (serviceLoader == null) {
				LOG.info("Loading module ''{0}''...", moduleName);

				// TODO
				serviceLoader = Thread.currentThread().getContextClassLoader();
			}
		} else {
			serviceLoader = Thread.currentThread().getContextClassLoader();
		}
		return serviceLoader;
	}

}
