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
package de.carne.lwjsd.runtime.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.carne.boot.ApplicationJarClassLoader;
import de.carne.boot.logging.Log;
import de.carne.io.Closeables;
import de.carne.lwjsd.api.ModuleInfo;
import de.carne.lwjsd.api.ModuleState;
import de.carne.lwjsd.api.ReasonMessage;
import de.carne.lwjsd.api.Service;
import de.carne.lwjsd.api.ServiceContext;
import de.carne.lwjsd.api.ServiceException;
import de.carne.lwjsd.api.ServiceId;
import de.carne.lwjsd.api.ServiceInfo;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceState;
import de.carne.lwjsd.runtime.config.Config;
import de.carne.lwjsd.runtime.security.SecretsStore;
import de.carne.lwjsd.runtime.security.Signature;
import de.carne.nio.file.attribute.FileAttributes;
import de.carne.util.function.FunctionException;

final class ServiceStore {

	private static final Log LOG = new Log();

	private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper()
			.enable(SerializationFeature.INDENT_OUTPUT);

	private static final String STATE_FILE = "lwjsd.services.json";
	private static final String MODULES_DIR = "modules";

	public static final String RUNTIME_MODULE_NAME = "";
	public static final String RUNTIME_MODULE_VERSION = "";

	public static final Pattern MODULE_FILE_NAME_PATTERN = Pattern.compile("(.+)-(\\d+\\.\\d+\\.\\d+)\\.jar");

	private final ModuleFactory moduleFactory = this::getCachedModule;
	private final ServiceFactory serviceFactory = this::getCachedService;
	private final Map<String, ClassLoader> moduleCache = new HashMap<>();
	private final Map<ServiceId, Service> serviceCache = new HashMap<>();
	private final Map<String, ModuleInstance> moduleInstances = new HashMap<>();
	private final Map<ServiceId, ServiceInstance> serviceInstances = new HashMap<>();
	private final SecretsStore secretsStore;
	private final ServiceContext serviceContext;
	private final Path modulesDir;
	private final Path stateFile;

	private ServiceStore(SecretsStore secretsStore, ServiceContext serviceContext, Path modulesDir, Path stateFile) {
		this.secretsStore = secretsStore;
		this.serviceContext = serviceContext;
		this.modulesDir = modulesDir;
		this.stateFile = stateFile;
		this.moduleCache.put(RUNTIME_MODULE_NAME, getClass().getClassLoader());
	}

	public static ServiceStore create(SecretsStore secretsStore, ServiceContext serviceContext, Config config)
			throws IOException {
		Path stateDir = config.getStateDir();

		Files.createDirectories(stateDir, FileAttributes.userDirectoryDefault(stateDir));

		Path modulesDir = stateDir.resolve(MODULES_DIR);

		LOG.info("Using modules directory ''{0}''...", modulesDir);

		Files.createDirectories(modulesDir, FileAttributes.userDirectoryDefault(stateDir));

		Path stateFile = stateDir.resolve(STATE_FILE);

		LOG.info("Using state file ''{0}''...", stateFile);

		ServiceStore serviceStore = new ServiceStore(secretsStore, serviceContext, modulesDir, stateFile);

		serviceStore.restoreModuleRegistrations();
		if (Files.exists(stateFile)) {
			JsonServiceStore json = JSON_OBJECT_MAPPER.readValue(stateFile.toFile(), JsonServiceStore.class);

			for (JsonServiceStoreService jsonService : json.getServices()) {
				serviceStore.restoreServiceRegistration(jsonService);
			}
		}
		serviceStore.autoDiscoverModuleServices(RUNTIME_MODULE_NAME);
		serviceStore.syncStore0();
		return serviceStore;
	}

	private void syncStore0() throws IOException {
		Collection<JsonServiceStoreService> jsonServices = new ArrayList<>(this.serviceInstances.size());

		for (ServiceInstance serviceInstance : this.serviceInstances.values()) {
			ServiceId serviceId = serviceInstance.id();
			JsonServiceStoreService jsonService = new JsonServiceStoreService(serviceId.moduleName(),
					serviceId.serviceName(), serviceInstance.getAutoStartFlag());

			jsonServices.add(jsonService);
		}

		JsonServiceStore json = new JsonServiceStore(jsonServices);

		JSON_OBJECT_MAPPER.writeValue(this.stateFile.toFile(), json);

		LOG.info("Service states have been written to file ''{0}''", this.stateFile);
	}

	public synchronized void syncStore() throws ServiceManagerException {
		try {
			syncStore0();
		} catch (IOException e) {
			throw new ServiceManagerException(e, "Failed to write service states file ''{0}''", this.stateFile);
		}
	}

	public synchronized Collection<ModuleInfo> queryModuleStatus() {
		Collection<ModuleInfo> moduleInfos = new ArrayList<>(this.moduleInstances.size());

		for (ModuleInstance moduleInstance : this.moduleInstances.values()) {
			moduleInfos.add(new ModuleInfo(moduleInstance.name(), moduleInstance.version(), moduleInstance.getState()));
		}
		return moduleInfos;
	}

	public synchronized Collection<ServiceInfo> queryServiceStatus() {
		Collection<ServiceInfo> serviceInfos = new ArrayList<>(this.serviceInstances.size());

		for (ServiceInstance serviceInstance : this.serviceInstances.values()) {
			serviceInfos.add(new ServiceInfo(serviceInstance.id(), serviceInstance.getState(),
					serviceInstance.getAutoStartFlag()));
		}
		return serviceInfos;
	}

	public synchronized ModuleInfo registerModule(Path file, boolean force) throws ServiceManagerException {
		LOG.info("Registering module ''{0}''...", file);

		Matcher moduleNameMatcher = MODULE_FILE_NAME_PATTERN.matcher(file.getFileName().toString());

		if (!moduleNameMatcher.matches()) {
			throw new ServiceManagerException(
					ReasonMessage.illegalArgument("Failed to register invalidly named module ''{0}''", file));
		}

		String moduleName = moduleNameMatcher.group(1);
		String moduleVersion = moduleNameMatcher.group(2);

		ModuleInstance moduleInstance = this.moduleInstances.get(moduleName);

		if (moduleInstance != null) {
			if (!force && moduleInstance.version().compareTo(moduleVersion) >= 0) {
				throw new ServiceManagerException(
						ReasonMessage.illegalState("Failed to register outdated module ''{0}'' (version: {1} <= {2})",
								moduleName, moduleVersion, moduleInstance.version()));
			}
			deleteModule(moduleName);
		}
		try {
			installModule(file);
		} catch (IOException | GeneralSecurityException e) {
			throw new ServiceManagerException(e, "Failed to install module ''{0}''", file);
		}
		this.moduleInstances.put(moduleName, new ModuleInstance(this.moduleFactory, moduleName, moduleVersion));
		return loadModule(moduleName);
	}

	@SuppressWarnings("squid:S1301")
	public synchronized ModuleInfo loadModule(String moduleName) throws ServiceManagerException {
		LOG.info("Loading module ''{0}''...", moduleName);

		ModuleInstance moduleInstance = this.moduleInstances.get(moduleName);

		if (moduleInstance == null) {
			throw new ServiceManagerException(
					ReasonMessage.illegalArgument("Failed to load unknown module ''{0}''", moduleName));
		}

		ModuleState moduleState = moduleInstance.getState();

		while (moduleState != ModuleState.LOADED) {
			switch (moduleState) {
			case REGISTERED:
				moduleInstance.module();
				autoDiscoverModuleServices(moduleName);
				moduleInstance.setState(ModuleState.LOADED);
				break;
			case LOADED:
				LOG.info("Module ''{0}'' already loaded", moduleName);
				break;
			}
			moduleState = moduleInstance.getState();
		}
		return new ModuleInfo(moduleInstance.name(), moduleInstance.version(), moduleInstance.getState());
	}

	public synchronized void deleteModule(String moduleName) throws ServiceManagerException {
		LOG.info("Deleting module ''{0}''...", moduleName);

		ModuleInstance moduleInstance = this.moduleInstances.get(moduleName);

		if (moduleInstance == null) {
			throw new ServiceManagerException(
					ReasonMessage.illegalArgument("Failed to delete unknown module ''{0}''", moduleName));
		}

		Iterator<ServiceInstance> serviceInstancesIterator = this.serviceInstances.values().iterator();

		while (serviceInstancesIterator.hasNext()) {
			ServiceInstance serviceInstance = serviceInstancesIterator.next();
			ServiceId serviceId = serviceInstance.id();

			if (serviceId.moduleName().equals(moduleName)) {
				stopService(serviceId, true);
				serviceInstancesIterator.remove();
				this.serviceCache.remove(serviceId);
			}
		}
		this.moduleCache.remove(moduleName);
		this.moduleInstances.remove(moduleName);

		String moduleFileName = moduleInstance.fileName();

		try (Stream<Path> paths = Files.walk(this.modulesDir, 1)) {
			paths.forEach(path -> {
				if (path.getFileName().toString().startsWith(moduleFileName)) {
					try {
						Files.delete(path);
					} catch (IOException e) {
						throw new FunctionException(e);
					}
				}
			});
		} catch (IOException e) {
			throw new ServiceManagerException(e, "Failed to determine module ''{0}'' files", moduleName);
		} catch (FunctionException e) {
			throw new ServiceManagerException(e.getCause(), "Failed to delete module ''{0}'' files", moduleName);
		}

		LOG.info("Module ''{0}'' deleted", moduleName);
	}

	public synchronized ServiceInfo registerService(ServiceId serviceId, boolean autoStartFlag) {
		return registerService0(serviceId, null, autoStartFlag);
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
			throw new ServiceManagerException(
					ReasonMessage.illegalArgument("Failed to get service of type {0}", serviceClass.getName()));
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
				LOG.warning(e, "Failed to auto start service ''{0}''", autoStartServiceId);
			}
		}
	}

	public synchronized ServiceInfo startService(ServiceId serviceId, boolean autoStart)
			throws ServiceManagerException {
		LOG.info("Starting service ''{0}''...", serviceId);

		ServiceInstance serviceInstance = this.serviceInstances.get(serviceId);

		if (serviceInstance == null) {
			throw new ServiceManagerException(
					ReasonMessage.illegalArgument("Failed to start unknown service ''{0}''", serviceId));
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
		return new ServiceInfo(serviceInstance.id(), serviceInstance.getState(), serviceInstance.getAutoStartFlag());
	}

	public synchronized ServiceInfo stopService(ServiceId serviceId, boolean unload) throws ServiceManagerException {
		LOG.info("Stopping service ''{0}''...", serviceId);

		ServiceInstance serviceInstance = this.serviceInstances.get(serviceId);

		if (serviceInstance == null) {
			throw new ServiceManagerException(
					ReasonMessage.illegalArgument("Failed to stop unknown service ''{0}''", serviceId));
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
		return new ServiceInfo(serviceInstance.id(), serviceInstance.getState(), serviceInstance.getAutoStartFlag());
	}

	public synchronized void safeUnloadAllServices() {
		LOG.info("Unloading all services...");

		for (ServiceInstance serviceInstance : this.serviceInstances.values()) {
			ServiceId serviceId = serviceInstance.id();

			try {
				stopService(serviceId, true);
			} catch (ServiceManagerException e) {
				LOG.warning(e, "Failed to unload service ''{0}''", serviceId);
			}
		}
	}

	public synchronized void close() {
		this.serviceInstances.clear();
		this.serviceCache.clear();
		this.moduleCache.values().forEach(Closeables::safeClose);
		this.moduleCache.clear();
	}

	private void restoreModuleRegistrations() throws IOException {
		LOG.info("Scanning for registered modules in directory ''{0}''...", this.modulesDir);

		try (Stream<Path> paths = Files.walk(this.modulesDir, 1)) {
			paths.forEach(path -> {
				Matcher matcher = MODULE_FILE_NAME_PATTERN.matcher(path.getFileName().toString());

				if (matcher.matches()) {
					String moduleName = matcher.group(1);
					String moduleVersion = matcher.group(2);

					this.moduleInstances.put(moduleName,
							new ModuleInstance(this.moduleFactory, moduleName, moduleVersion));

					LOG.info("Module ''{0}'' registered", moduleName);
				}
			});
		}
	}

	private void restoreServiceRegistration(JsonServiceStoreService jsonService) {
		ServiceId serviceId = new ServiceId(jsonService.getModuleName(), jsonService.getServiceName());

		LOG.info("Restoring service registration ''{0}''...", serviceId);

		registerService0(serviceId, null, jsonService.getAutoStartFlag());
	}

	private ServiceInfo registerService0(ServiceId serviceId, @Nullable Service service, boolean autoStartFlag) {
		LOG.info("Registering service ''{0}''...", serviceId);

		@Nullable ServiceInstance serviceInstance = this.serviceInstances.get(serviceId);

		if (serviceInstance == null) {
			serviceInstance = new ServiceInstance(this.serviceFactory, serviceId, autoStartFlag);
			if (service != null) {
				this.serviceCache.put(serviceId, service);
			}
			this.serviceInstances.put(serviceId, serviceInstance);

			LOG.notice("Service ''{0}'' registered", serviceId);
		} else {
			LOG.info("Service ''{0}'' already registered", serviceId);
		}
		return new ServiceInfo(serviceInstance.id(), serviceInstance.getState(), serviceInstance.getAutoStartFlag());
	}

	private void autoDiscoverModuleServices(String moduleName) {
		LOG.info("Auto discovering services for module ''{0}''...", moduleName);

		ClassLoader loader = Objects.requireNonNull(this.moduleCache.get(moduleName));
		ServiceLoader<Service> services = ServiceLoader.load(Service.class, loader);

		for (Service service : services) {
			ServiceId serviceId = new ServiceId(moduleName, service);

			registerService0(serviceId, service, true);
		}
	}

	private ClassLoader getCachedModule(String moduleName) throws ServiceManagerException {
		ClassLoader loader = this.moduleCache.get(moduleName);

		if (loader == null) {
			LOG.info("Instantiating module ''{0}''...", moduleName);

			ModuleInstance moduleInstance = Objects.requireNonNull(this.moduleInstances.get(moduleName));
			String moduleFileName = moduleInstance.fileName();
			String signaturePrefix = moduleFileName + ".";
			Collection<String> moduleSignatures;

			try (Stream<Path> paths = Files.walk(this.modulesDir, 1)) {
				moduleSignatures = paths.map(path -> path.getFileName().toString())
						.filter(path -> path.startsWith(signaturePrefix)).collect(Collectors.toList());
			} catch (IOException e) {
				throw new ServiceManagerException(e, "Failed to scan signatures for module ''{0}''", moduleName);
			}
			if (moduleSignatures.isEmpty()) {
				throw new ServiceManagerException("Failed to find signature(s) for module ''{0}''", moduleName);
			}
			try {
				for (String signatureFileName : moduleSignatures) {
					if (!verifyModule(moduleFileName, signatureFileName)) {
						throw new ServiceManagerException("Failed to verify module ''{0}''", moduleName);
					}
				}

				Path moduleFile = this.modulesDir.resolve(moduleFileName);

				loader = new ApplicationJarClassLoader(moduleFile.toFile(), this.moduleCache.get(RUNTIME_MODULE_NAME));
			} catch (IOException | GeneralSecurityException e) {
				throw new ServiceManagerException(e, "Failed to instantiate module ''{0}''", moduleName);
			}
			this.moduleCache.put(moduleName, loader);
		}
		return loader;
	}

	private Service getCachedService(ServiceId serviceId) throws ServiceManagerException {
		Service service = this.serviceCache.get(serviceId);

		if (service == null) {
			LOG.info("Instantiating service ''{0}''...", serviceId);

			ClassLoader loader = getCachedModule(serviceId.moduleName());

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

	private void installModule(Path file) throws IOException, GeneralSecurityException {
		Signature signature = this.secretsStore.getDefaultSignature();

		String fileName = file.getFileName().toString();
		Path moduleFile = this.modulesDir.resolve(fileName);
		Path signatureFile = this.modulesDir.resolve(fileName + "." + signature.name());

		try (InputStream fileStream = Files.newInputStream(file);
				CopyStream copyStream = new CopyStream(fileStream, moduleFile);
				OutputStream signatureStream = Files.newOutputStream(signatureFile, StandardOpenOption.CREATE)) {
			byte[] signatureBytes = signature.sign(copyStream);

			signatureStream.write(signatureBytes);
		}
	}

	private boolean verifyModule(String moduleFileName, String signatureFileName)
			throws IOException, GeneralSecurityException {
		Path modulePath = this.modulesDir.resolve(moduleFileName);
		Path signaturePath = this.modulesDir.resolve(signatureFileName);

		LOG.debug("Verifying module file ''{0}'' using signature file ''{1}''...", modulePath, signaturePath);

		String signatureName = signatureFileName.substring(moduleFileName.length() + 1);
		Signature signature = this.secretsStore.getSignature(signatureName);
		byte[] signatureBytes = Files.readAllBytes(signaturePath);
		boolean verified;

		try (InputStream moduleStream = Files.newInputStream(modulePath)) {
			verified = signature.verify(moduleStream, signatureBytes);
		}
		return verified;
	}

	private static class ModuleInstance {

		private final ModuleFactory factory;
		private final String name;
		private final String version;
		private ModuleState state = ModuleState.REGISTERED;

		ModuleInstance(ModuleFactory factory, String name, String version) {
			this.factory = factory;
			this.name = name;
			this.version = version;
		}

		public String name() {
			return this.name;
		}

		public String version() {
			return this.version;
		}

		public String fileName() {
			return this.name + "-" + this.version + ".jar";
		}

		public ClassLoader module() throws ServiceManagerException {
			return this.factory.get(this.name);
		}

		public ModuleState getState() {
			return this.state;
		}

		public void setState(ModuleState state) {
			this.state = state;
		}

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
			return Objects.requireNonNull(this.services);
		}

		// Implicitly used by ObjectMapper
		@SuppressWarnings("unused")
		public void setServices(Collection<JsonServiceStoreService> services) {
			this.services = services;
		}

	}

	private static final class JsonServiceStoreService {

		@Nullable
		private String moduleName;
		@Nullable
		private String serviceName;
		private boolean autoStartFlag;

		// Implicitly used by ObjectMapper
		@SuppressWarnings("unused")
		public JsonServiceStoreService() {
			// Nothing to do here
		}

		public JsonServiceStoreService(String moduleName, String serviceName, boolean autoStartFlag) {
			this.moduleName = moduleName;
			this.serviceName = serviceName;
			this.autoStartFlag = autoStartFlag;
		}

		public String getModuleName() {
			return Objects.requireNonNull(this.moduleName);
		}

		// Implicitly used by ObjectMapper
		@SuppressWarnings("unused")
		public void setModuleName(String moduleName) {
			this.moduleName = moduleName;
		}

		public String getServiceName() {
			return Objects.requireNonNull(this.serviceName);
		}

		// Implicitly used by ObjectMapper
		@SuppressWarnings("unused")
		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
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
