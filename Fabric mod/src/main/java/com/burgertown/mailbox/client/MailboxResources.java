package com.burgertown.mailbox.client;

import com.burgertown.mailbox.MailboxMod;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.util.Optional;

final class MailboxResources {
	private MailboxResources() {
	}

	static JsonObject readJson(String path) {
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(MailboxMod.id(path));
		if (resource.isEmpty()) {
			throw new IllegalStateException("Missing mailbox resource: " + path);
		}
		try {
			return JsonParser.parseString(resource.get().readAllAsString()).getAsJsonObject();
		} catch (IOException e) {
			throw new IllegalStateException("Could not read mailbox resource: " + path, e);
		}
	}
}
