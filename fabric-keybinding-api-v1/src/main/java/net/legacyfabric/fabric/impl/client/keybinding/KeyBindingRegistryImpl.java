/*
 * Copyright (c) 2016-2021 FabricMC
 * Copyright (c) 2020-2021 Legacy Fabric
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

package net.legacyfabric.fabric.impl.client.keybinding;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import net.legacyfabric.fabric.mixin.keybinding.KeyBindingAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.options.KeyBinding;

public final class KeyBindingRegistryImpl {
	private static final Logger LOGGER = LogManager.getLogger();

	private static final List<KeyBinding> moddedKeyBindings = Lists.newArrayList();

	private KeyBindingRegistryImpl() {
	}

	private static Map<String, Integer> getCategoryMap() {
		return KeyBindingAccessor.getCategoryMap();
	}

	private static boolean hasCategory(String categoryTranslationKey) {
		return getCategoryMap().containsKey(categoryTranslationKey);
	}

	public static boolean addCategory(String categoryTranslationKey) {
		Map<String, Integer> map = getCategoryMap();

		if (map.containsKey(categoryTranslationKey)) {
			return false;
		}

		Optional<Integer> largest = map.values().stream().max(Integer::compareTo);
		int largestInt = largest.orElse(0);
		map.put(categoryTranslationKey, largestInt + 1);
		return true;
	}

	public static KeyBinding registerKeyBinding(KeyBinding binding) {
		for (KeyBinding existingKeyBindings : moddedKeyBindings) {
			if (existingKeyBindings == binding) {
				throw new RuntimeException("Attempted to register same key binding twice: " + binding.getId() + "!");
			} else if (existingKeyBindings.getId().equals(binding.getId())) {
				throw new RuntimeException("Attempted to register two key bindings with equal ID: " + binding.getId() + "!");
			}
		}

		if (!hasCategory(binding.getCategory())) {
			addCategory(binding.getCategory());
		}

		return moddedKeyBindings.add(binding) ? binding : null;
	}

	/**
	 * Processes the keybindings array for our modded ones by first removing existing modded keybindings and readding them,
	 * we can make sure that there are no duplicates this way.
	 */
	public static KeyBinding[] process(KeyBinding[] keysAll) {
		List<KeyBinding> newKeysAll = Lists.newArrayList(keysAll);
		newKeysAll.removeAll(moddedKeyBindings);
		newKeysAll.addAll(moddedKeyBindings);
		return newKeysAll.toArray(new KeyBinding[0]);
	}
}
