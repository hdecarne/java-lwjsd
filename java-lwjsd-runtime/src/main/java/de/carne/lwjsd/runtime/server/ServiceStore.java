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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.carne.check.Check;
import de.carne.check.Nullable;
import de.carne.io.Closeables;
import de.carne.lwjsd.api.ModuleInfo;
import de.carne.lwjsd.api.ModuleState;
import de.carne.lwjsd.api.Service;
import de.carne.lwjsd.api.ServiceContext;
import de.carne.lwjsd.api.ServiceException;
import de.carne.lwjsd.api.ServiceId;
import de.carne.lwjsd.api.ServiceInfo;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceState;
import de.carne.lwjsd.runtime.config.Config;
import de.carne.lwjsd.runtime.ws.JsonServiceId;
import de.carne.nio.file.attribute.FileAttributes;
import de.carne.util.Exceptions;
import de.carne.util.logging.Log;

final class ServiceStore {

	private static final Log LOG = new Log();

	private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper()
			.enable(SerializationFeature.INDENT_OUTPUT);

	private static final String STATE_FILE = "lwjsd.services.json";
	private static final String MODULES_DIR = "modules";

	public static final String RUNTIME_MODULE_NAME = "";

	private final ServiceFactory serviceFactory = this::getService;
	private final Map<String, ClassLoader> moduleCache = new HashMap<>();
	private final Map<ServiceId, Service> serviceCache = new HashMap<>();
	private final Map<ServiceId, ServiceInstance> serviceInstances = new HashMap<>();
	private final ServiceContext serviceContext;
	private final Path modulesDir;
	private final Path stateFile;

	private ServiceStore(ServiceContext serviceContext, Path modulesDir, Path stateFile) {
		this.serviceContext = serviceContext;
		this.modulesDir = modulesDir;
		this.stateFile = stateFile;
		this.moduleCache.put(RUNTIME_MODULE_NAME, getClass().getClassLoader());
	}

	public static ServiceStore create(ServiceContext serviceContext, Config config) throws IOException {
		Path stateDir = config.getStateDir();

		Files.createDirectories(stateDir, FileAttributes.userDirectoryDefault(stateDir));

		Path modulesDir = stateDir.resolve(MODULES_DIR);

		LOG.info("Using modules directory ''{0}''...", modulesDir);

		Files.createDirectories(modulesDir, FileAttributes.userDirectoryDefault(stateDir));

		Path stateFile = stateDir.resolve(STATE_FILE);

		LOG.info("Using state file ''{0}''...", stateFile);

		ServiceStore serviceStore = new ServiceStore(serviceContext, modulesDir, stateFile);

		if (Files.exists(stateFile)) {
			JsonServiceStore json = JSON_OBJECT_MAPPER.readValue(stateFile.toFile(), JsonServiceStore.class);

			for (JsonServiceStoreService jsonService : json.getServices()) {
				serviceStore.restoreServiceRegistration(jsonService);
			}
		}
		serviceStore.autoDiscoverServices(RUNTIME_MODULE_NAME);
		serviceStore.syncStore0();
		return serviceStore;
	}

	private void syncStore0() throws IOException {
		Collection<JsonServiceStoreService> jsonServices = new ArrayList<>(this.serviceInstances.size());

		for (ServiceInstance serviceInstance : this.serviceInstances.values()) {
			JsonServiceId jsonServiceId = new JsonServiceId(serviceInstance.id());
			JsonServiceStoreService jsonService = new JsonServiceStoreService(jsonServiceId,
					serviceInstance.getAutoStartFlag());

			jsonServices.add(jsonService);
		}

		JsonServiceStore json = new JsonServiceStore(jsonServices);

		JSON_OBJECT_MAPPER.writeValue(this.stateFile.toFile(), json);

		LOG.info("Service states have been written to file ''{0}''", this.stateFile);
	}

	public void syncStore() throws ServiceManagerException {
		try {
			syncStore0();
		} catch (IOException e) {
			throw new ServiceManagerException(e, "Failed to write service states file ''{0}''", this.stateFile);
		}
	}

	public synchronized Collection<ModuleInfo> queryModuleStatus() throws ServiceManagerException {
		Collection<ModuleInfo> moduleInfos;

		try (Stream<Path> paths = Files.walk(this.modulesDir, 0)) {
			moduleInfos = paths.filter(path -> path.toString().endsWith(".jar")).map(path -> {
				String moduleName = path.getFileName().toString();
				ModuleState moduleState = (this.moduleCache.containsKey(moduleName) ? ModuleState.LOADED
						: ModuleState.REGISTERED);

				return new ModuleInfo(moduleName, moduleState);
			}).collect(Collectors.toList());
		} catch (IOException e) {
			throw new ServiceManagerException(e, "Failed to scan registered modules");
		}
		return moduleInfos;
	}

	public synchronized Collection<ServiceInfo> queryServiceStatus() {
		Collection<ServiceInfo> serviceInfos = new ArrayList<>();

		for (ServiceInstance serviceInstance : this.serviceInstances.values()) {
			serviceInfos.add(new ServiceInfo(serviceInstance.id(), serviceInstance.getState(),
					serviceInstance.getAutoStartFlag()));
		}
		return serviceInfos;
	}

	public synchronized void registerService(ServiceId serviceId, boolean autoStartFlag) {
		registerService0(serviceId, null, autoStartFlag);
	}

	public synchronized <T extends Service> T getService(Class<T> serviceClass) throws ServiceManagerException {
		Service foundService = null;

		for (Service service : this.serviceCache.values()) {
			if (serviceClass.isAssignableFrom(service.getClass())) {
				foundService = service;
				break;
			}
		}
		if (foundService == null) {
			throw new ServiceManagerException("Failed to get service of type {0}", serviceClass.getName());
		}
		return serviceClass.cast(foundService);
	}

	public synchronized void autoStartServices() {
		LOG.info("Auto starting services...");

		// Copy affected ids first, as implicit module loading may register new services
		Collection<ServiceId> autoStartServiceIds = this.serviceInstances.values().stream()
				.filter(ServiceInstance::getAutoStartFlag).map(ServiceInstance::id).collect(Collectors.toList());

		for (ServiceId autoStartServiceId : autoStartServiceIds) {
			try {
				startService(autoStartServiceId, true);
			} catch (ServiceManagerException e) {
				LOG.warning(e, Exceptions.toString(e));
			}
		}
	}

	public synchronized void startService(ServiceId serviceId, boolean autoStart) throws ServiceManagerException {
		LOG.info("Starting service ''{0}''...", serviceId);

		ServiceInstance serviceInstance = this.serviceInstances.get(serviceId);

		if (serviceInstance == null) {
			throw new ServiceManagerException("Failed to start unknown service ''{0}''", serviceId);
		}

		ServiceState serviceState = serviceInstance.getState();

		while (serviceState != ServiceState.RUNNING) {
			switch (serviceState) {
			case REGISTERED:
				LOG.info("Loading service ''{0}''...", serviceId);

				try {
					serviceInstance.service().load(this.serviceContext);
				} catch (ServiceException e) {
					throw new ServiceManagerException(e, "Failed to load service ''{0}''", serviceId);
				}
				serviceInstance.setState(ServiceState.LOADED);

				LOG.notice("Service ''{0}'' loaded", serviceId);
				break;
			case LOADED:
				try {
					serviceInstance.service().start(this.serviceContext);
				} catch (ServiceException e) {
					throw new ServiceManagerException(e, "Failed to start service ''{0}''", serviceId);
				}
				serviceInstance.setAutoStartFlag(autoStart);
				serviceInstance.setState(ServiceState.RUNNING);

				LOG.notice("Service ''{0}'' up and running", serviceId);
				break;
			case RUNNING:
				LOG.info("Service ''{0}'' already up and running", serviceId);
				break;
			}
			serviceState = serviceInstance.getState();
		}
	}

	public synchronized void stopService(ServiceId serviceId, boolean unload) throws ServiceManagerException {
		LOG.info("Stopping service ''{0}''...", serviceId);

		ServiceInstance serviceInstance = this.serviceInstances.get(serviceId);

		if (serviceInstance == null) {
			throw new ServiceManagerException("Failed to stop unknown service ''{0}''", serviceId);
		}

		ServiceState targetServiceState = (unload ? ServiceState.REGISTERED : ServiceState.LOADED);
		ServiceState serviceState = serviceInstance.getState();

		while (serviceState != ServiceState.REGISTERED && serviceState != targetServiceState) {
			switch (serviceState) {
			case RUNNING:
				try {
					serviceInstance.service().stop(this.serviceContext);
				} catch (ServiceException e) {
					throw new ServiceManagerException(e, "Failed to stop service ''{0}''", serviceId);
				}
				serviceInstance.setState(ServiceState.LOADED);

				LOG.notice("Service ''{0}'' has been stopped", serviceId);
				break;
			case LOADED:
				try {
					serviceInstance.service().unload(this.serviceContext);
				} catch (ServiceException e) {
					throw new ServiceManagerException(e, "Failed to unload service ''{0}''", serviceId);
				}
				serviceInstance.setState(ServiceState.REGISTERED);

				LOG.notice("Service ''{0}'' has been unloaded", serviceId);
				break;
			case REGISTERED:
				LOG.info("Service ''{0}'' already stopped and unloaded", serviceId);
				break;
			}
			serviceState = serviceInstance.getState();
		}
	}

	public synchronized void unloadAllServices() {
		LOG.info("Unloading all services...");

		for (ServiceInstance serviceInstance : this.serviceInstances.values()) {
			try {
				stopService(serviceInstance.id(), true);
			} catch (ServiceManagerException e) {
				LOG.warning(e, Exceptions.toString(e));
			}
		}
	}

	public synchronized void close() {
		this.serviceInstances.clear();
		this.serviceCache.clear();
		this.moduleCache.values().forEach(Closeables::safeClose);
		this.moduleCache.clear();
	}

	private void restoreServiceRegistration(JsonServiceStoreService jsonService) {
		ServiceId serviceId = jsonService.getId().toSource();

		LOG.info("Restoring service registration ''{0}''...", serviceId);

		registerService0(serviceId, null, jsonService.getAutoStartFlag());
	}

	private void registerService0(ServiceId serviceId, @Nullable Service service, boolean autoStartFlag) {
		LOG.info("Registering service ''{0}''...", serviceId);

		if (!this.serviceInstances.containsKey(serviceId)) {
			if (service != null) {
				this.serviceCache.put(serviceId, service);
			}
			this.serviceInstances.put(serviceId, new ServiceInstance(this.serviceFactory, serviceId, autoStartFlag));

			LOG.notice("Service ''{0}'' registered", serviceId);
		} else {
			LOG.info("Service ''{0}'' already registered", serviceId);
		}
	}

	private ClassLoader loadModule(String moduleName) {
		ClassLoader loader = this.moduleCache.get(moduleName);

		if (loader == null) {
			throw Check.fail();
		}
		return loader;
	}

	private void autoDiscoverServices(String moduleName) {
		LOG.info("Auto discovering services for module ''{0}''...", moduleName);

		ClassLoader loader = this.moduleCache.get(moduleName);
		ServiceLoader<Service> services = ServiceLoader.load(Service.class, loader);

		for (Service service : services) {
			ServiceId serviceId = new ServiceId(moduleName, service);

			registerService0(serviceId, service, true);
		}
	}

	private Service getService(ServiceId serviceId) throws ServiceManagerException {
		Service service = this.serviceCache.get(serviceId);

		if (service == null) {
			LOG.info("Instantiating service ''{0}''...", serviceId);

			ClassLoader loader = loadModule(serviceId.moduleName());

			try {
				service = loader.loadClass(serviceId.serviceName()).asSubclass(Service.class).getConstructor()
						.newInstance();
				this.serviceCache.put(serviceId, service);
			} catch (ReflectiveOperationException e) {
				throw new ServiceManagerException(e, "Failed to instantiate service ''{0}''", serviceId);
			}
		}
		return service;
	}

	private static class ServiceInstance {

		private final ServiceFactory factory;
		private final ServiceId id;
		private boolean autoStartFlag;
		private ServiceState state = ServiceState.REGISTERED;

		ServiceInstance(ServiceFactory factory, ServiceId id, boolean autoStartFlag) {
			this.factory = factory;
			this.id = id;
			this.autoStartFlag = autoStartFlag;
		}

		public ServiceId id() {
			return this.id;
		}

		public Service service() throws ServiceManagerException {
			return this.factory.get(this.id);
		}

		public boolean getAutoStartFlag() {
			return this.autoStartFlag;
		}

		public void setAutoStartFlag(boolean autoStartFlag) {
			this.autoStartFlag = autoStartFlag;
		}

		public ServiceState getState() {
			return this.state;
		}

		public void setState(ServiceState state) {
			this.state = state;
		}

	}

	private static final class JsonServiceStore {

		@Nullable
		private Collection<JsonServiceStoreService> services;

		// Implicitly used by ObjectMapper
		@SuppressWarnings("unused")
		public JsonServiceStore() {
			// Nothing to do here
		}

		public JsonServiceStore(Collection<JsonServiceStoreService> services) {
			this.services = services;
		}

		public Collection<JsonServiceStoreService> getServices() {
			return Check.notNull(this.services);
		}

		// Implicitly used by ObjectMapper
		@SuppressWarnings("unused")
		public void setServices(Collection<JsonServiceStoreService> services) {
			this.services = services;
		}

	}

	private static final class JsonServiceStoreService {

		@Nullable
		private JsonServiceId id;
		private boolean autoStartFlag;

		// Implicitly used by ObjectMapper
		@SuppressWarnings("unused")
		public JsonServiceStoreService() {
			// Nothing to do here
		}

		public JsonServiceStoreService(JsonServiceId id, boolean autoStartFlag) {
			this.id = id;
			this.autoStartFlag = autoStartFlag;
		}

		public JsonServiceId getId() {
			return Check.notNull(this.id);
		}

		// Implicitly used by ObjectMapper
		@SuppressWarnings("unused")
		public void setId(JsonServiceId id) {
			this.id = id;
		}

		public boolean getAutoStartFlag() {
			return this.autoStartFlag;
		}

		// Implicitly used by ObjectMapper
		@SuppressWarnings("unused")
		public void setAutoStartFlag(boolean autoStartFlag) {
			this.autoStartFlag = autoStartFlag;
		}

	}

}
