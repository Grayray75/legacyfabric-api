/*
 * Copyright (c) 2020 - 2022 Legacy Fabric
 * Copyright (c) 2016 - 2022 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.legacyfabric.fabric.impl.registry.sync;

import java.util.function.IntSupplier;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.SimpleRegistry;

import net.legacyfabric.fabric.api.logger.v1.Logger;
import net.legacyfabric.fabric.impl.logger.LoggerImpl;
import net.legacyfabric.fabric.impl.registry.RegistryHelperImpl;
import net.legacyfabric.fabric.impl.registry.sync.compat.IdListCompat;
import net.legacyfabric.fabric.impl.registry.sync.compat.SimpleRegistryCompat;

public class RegistryRemapper<T> {
	protected static final Logger LOGGER = Logger.get(LoggerImpl.API, "RegistryRemapper");
	protected final SimpleRegistry<Identifier, T> registry;
	protected BiMap<Identifier, Integer> entryDump;
	protected BiMap<Identifier, Integer> missingMap = HashBiMap.create();
	public final Identifier registryId;
	public final String type;

	public static final Identifier ITEMS = new Identifier("items");
	public static final Identifier BLOCKS = new Identifier("blocks");

	public RegistryRemapper(SimpleRegistry<Identifier, T> registry, Identifier registryId, String type) {
		this.registry = registry;
		this.registryId = registryId;
		this.type = type;
	}

	public void dump() {
		this.entryDump = HashBiMap.create();
		RegistryHelperImpl.getIdMap(this.registry).forEach((value, id) -> {
			Identifier key = RegistryHelperImpl.getObjects(this.registry).get(value);
			if (key != null) this.entryDump.put(key, id);
		});

		this.entryDump.putAll(this.missingMap);
	}

	public NbtCompound toNbt() {
		if (this.entryDump == null) {
			this.dump();
		}

		NbtCompound nbt = new NbtCompound();
		this.entryDump.forEach((key, value) -> nbt.putInt(key.toString(), value));
		return nbt;
	}

	public void readNbt(NbtCompound tag) {
		this.entryDump = HashBiMap.create();

		for (String key : tag.getKeys()) {
			Identifier identifier = new Identifier(key);
			int id = tag.getInt(key);
			this.entryDump.put(identifier, id);
		}
	}

	// Type erasure, ily
	public void remap() {
		LOGGER.info("Remapping registry %s", this.registryId.toString());
		IdListCompat<T> newList = ((SimpleRegistryCompat<Identifier, T>) this.registry).createIdList();

		this.entryDump.forEach((id, rawId) -> {
			T value = RegistryHelperImpl.getObjects(this.registry).inverse().get(id);

			if (value == null) {
				newList.setValue(null, rawId);
				LOGGER.warn("%s with id %s is missing!", this.type, id.toString());
				this.missingMap.put(id, rawId);
			} else {
				newList.setValue(value, rawId);
			}
		});

		IntSupplier currentSize = () -> RegistryHelperImpl.getIdMap(newList).size();
		IntSupplier previousSize = () -> RegistryHelperImpl.getObjects(this.registry).size();

		if (currentSize.getAsInt() > previousSize.getAsInt()) {
			if (this.missingMap.size() == 0) {
				throw new IllegalStateException("Registry size increased from " + previousSize.getAsInt() + " to " + currentSize.getAsInt() + " after remapping! This is not possible!");
			}
		} else if (currentSize.getAsInt() < previousSize.getAsInt()) {
			LOGGER.info("Adding " + (previousSize.getAsInt() - currentSize.getAsInt()) + " missing entries to registry");

			RegistryHelperImpl.getObjects(this.registry).keySet().stream().filter(obj -> newList.getInt(obj) == -1).forEach(missing -> {
				int id = RegistryHelperImpl.nextId(this.registry);

				while (newList.fromInt(id) != null) {
					id = RegistryHelperImpl.nextId(newList);

					T currentBlock = RegistryHelperImpl.getIdList(this.registry).fromInt(id);

					if (currentBlock != null && newList.getInt(currentBlock) == -1) {
						newList.setValue(currentBlock, id);
					}
				}

				if (newList.getInt(missing) == -1) {
					newList.setValue(missing, id);
				} else {
					id = newList.getInt(missing);
				}

				LOGGER.info("Adding %s %s with numerical id %d to registry", this.type, this.registry.getIdentifier(missing), id);
			});
		}

		if (currentSize.getAsInt() != previousSize.getAsInt() && this.missingMap.size() == 0) {
			throw new IllegalStateException("An error occured during remapping");
		}

		((SimpleRegistryCompat<Identifier, T>) this.registry).setIds(newList);
		this.dump();
		LOGGER.info("Remapped " + previousSize.getAsInt() + " entries");
	}
}
