package org.asf.razorwhip.sentinel.launcher.descriptors.http.preprocessors;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.io.ByteArrayInputStream;

import org.asf.connective.RemoteClient;
import org.asf.connective.impl.http_1_1.RemoteClientHttp_1_1;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.razorwhip.sentinel.launcher.LauncherUtils;
import org.asf.razorwhip.sentinel.launcher.descriptors.data.ServerEndpoints;
import org.asf.razorwhip.sentinel.launcher.descriptors.http.ContentServerRequestHandler.IPreProcessor;

import com.google.gson.JsonObject;

public class ApplicationManifestPreProcessor implements IPreProcessor {

	private JsonObject descriptorDef;

	public ApplicationManifestPreProcessor(JsonObject descriptorDef) {
		this.descriptorDef = descriptorDef;
	}

	@Override
	public boolean match(String path, String method, RemoteClient client, String contentType, HttpRequest request,
			HttpResponse response) {
		return path.toLowerCase().endsWith("/dwadragonsmain.xml");
	}

	@Override
	public InputStream preProcess(String path, String method, RemoteClient client, String contentType,
			HttpRequest request, HttpResponse response, InputStream source) throws IOException {
		// Read manifest
		byte[] manifestB = source.readAllBytes();
		source.close();

		// Decode
		String manifest = new String(manifestB, "UTF-8");

		// Load endpoints
		ServerEndpoints endpoints = null;
		if (LauncherUtils.hasTag("server_endpoints"))
			endpoints = LauncherUtils.getTag("server_endpoints").getValue(ServerEndpoints.class);
		if (endpoints == null)
			endpoints = new ServerEndpoints();

		// Get local IP
		String host = "localhost";
		if (client instanceof RemoteClientHttp_1_1) {
			RemoteClientHttp_1_1 cl = (RemoteClientHttp_1_1) client;
			Socket sock = cl.getSocket();

			// Get interface
			SocketAddress addr = sock.getLocalSocketAddress();
			if (addr instanceof InetSocketAddress) {
				InetSocketAddress iA = (InetSocketAddress) addr;
				host = iA.getAddress().getCanonicalHostName();
			}
		}

		// Handle URLs
		endpoints.achievementServiceEndpoint = processURL(endpoints.achievementServiceEndpoint, host);
		endpoints.commonServiceEndpoint = processURL(endpoints.commonServiceEndpoint, host);
		endpoints.contentserverServiceEndpoint = processURL(endpoints.contentserverServiceEndpoint, host);
		endpoints.groupsServiceEndpoint = processURL(endpoints.groupsServiceEndpoint, host);
		endpoints.itemstoremissionServiceEndpoint = processURL(endpoints.itemstoremissionServiceEndpoint, host);
		endpoints.messagingServiceEndpoint = processURL(endpoints.messagingServiceEndpoint, host);
		endpoints.userServiceEndpoint = processURL(endpoints.userServiceEndpoint, host);

		// Find urls
		JsonObject endpointList = descriptorDef.get("manifestSodEndpoints").getAsJsonObject();
		ServerEndpoints oldEndpoints = new ServerEndpoints();
		oldEndpoints.achievementServiceEndpoint = endpointList.get("achievement.api.jumpstart.com").getAsString();
		oldEndpoints.commonServiceEndpoint = endpointList.get("common.api.jumpstart.com").getAsString();
		oldEndpoints.contentserverServiceEndpoint = endpointList.get("contentserver.api.jumpstart.com").getAsString();
		oldEndpoints.groupsServiceEndpoint = endpointList.get("groups.api.jumpstart.com").getAsString();
		oldEndpoints.itemstoremissionServiceEndpoint = endpointList.get("itemstoremission.api.jumpstart.com")
				.getAsString();
		oldEndpoints.messagingServiceEndpoint = endpointList.get("messaging.api.jumpstart.com").getAsString();
		oldEndpoints.userServiceEndpoint = endpointList.get("user.api.jumpstart.com").getAsString();

		// Modify manifest
		manifest = manifest.replace(oldEndpoints.achievementServiceEndpoint, endpoints.achievementServiceEndpoint);
		manifest = manifest.replace(oldEndpoints.commonServiceEndpoint, endpoints.commonServiceEndpoint);
		manifest = manifest.replace(oldEndpoints.contentserverServiceEndpoint, endpoints.contentserverServiceEndpoint);
		manifest = manifest.replace(oldEndpoints.groupsServiceEndpoint, endpoints.groupsServiceEndpoint);
		manifest = manifest.replace(oldEndpoints.itemstoremissionServiceEndpoint,
				endpoints.itemstoremissionServiceEndpoint);
		manifest = manifest.replace(oldEndpoints.messagingServiceEndpoint, endpoints.messagingServiceEndpoint);
		manifest = manifest.replace(oldEndpoints.userServiceEndpoint, endpoints.userServiceEndpoint);

		// Modify sfs hosts
		JsonObject sfsHosts = descriptorDef.get("manifestSmartfoxEndpoints").getAsJsonObject();
		for (String sfsH : sfsHosts.keySet()) {
			manifest = manifest.replace("<MMOServer>" + sfsH + "</MMOServer>", "<MMOServer>"
					+ (endpoints.smartFoxHost.equals("localhost") ? host : endpoints.smartFoxHost) + "</MMOServer>");
			manifest = manifest.replace("<MMOServerPort>" + sfsHosts.get(sfsH).getAsInt() + "</MMOServerPort>",
					"<MMOServerPort>" + endpoints.smartFoxPort + "</MMOServerPort>");
		}

		// Return
		return new ByteArrayInputStream(manifest.getBytes("UTF-8"));
	}

	private String processURL(String url, String host) throws IOException {
		URL u = new URL(url);
		if (u.getHost().equals("localhost"))
			url = new URL(u.getProtocol(), host, u.getPort(), u.getFile()).toString();
		if (!url.endsWith("/"))
			url += "/";
		return url;
	}

}
