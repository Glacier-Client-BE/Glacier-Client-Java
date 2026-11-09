package net.glacier.social;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.SessionService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.util.UUID;

/**
 * Mojang session join used as the registry proof. The access token is passed only to Mojang's session service
 * inside joinServer; it is never stored, logged or sent to the registry.
 */
final class SessionAuth {
	private SessionAuth() {
	}

	static final class Credentials {
		private final UUID profileId;
		private final String name;
		private final String accessToken;
		private final SessionService service;

		private Credentials(UUID profileId, String name, String accessToken, SessionService service) {
			this.profileId = profileId;
			this.name = name;
			this.accessToken = accessToken;
			this.service = service;
		}

		UUID profileId() {
			return profileId;
		}

		String name() {
			return name;
		}

		@Override
		public String toString() {
			return "Credentials[" + name + "]";
		}
	}

	/** Reads the current account. Call on the client thread. Returns null when no usable session exists. */
	static Credentials capture() {
		Minecraft mc = Minecraft.getInstance();
		User user = mc.getUser();
		if (user == null) return null;
		String token = user.getAccessToken();
		if (token == null || token.isEmpty()) return null;
		return new Credentials(user.getProfileId(), user.getName(), token, mc.services().sessionService());
	}

	/** Performs the Mojang join for the given server id. Runs on the registry thread. */
	static void join(Credentials c, String serverId) throws AuthenticationException {
		c.service.joinServer(c.profileId, c.accessToken, serverId);
	}
}
