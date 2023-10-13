package org.asf.razorwhip.sentinel.launcher.descriptors.http.preprocessors;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.io.ByteArrayInputStream;
import java.io.File;

import org.asf.connective.RemoteClient;
import org.asf.connective.impl.http_1_1.RemoteClientHttp_1_1;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

import org.asf.razorwhip.sentinel.launcher.descriptors.http.ContentServerRequestHandler.IPreProcessor;

import com.google.gson.JsonObject;

public class XmlPreProcessor implements IPreProcessor {

	private JsonObject descriptorDef;

	public XmlPreProcessor(JsonObject descriptorDef) {
		this.descriptorDef = descriptorDef;
	}

	@Override
	public boolean match(String path, String method, RemoteClient client, String contentType, HttpRequest request,
			HttpResponse response, File sourceFile) {
		return path.toLowerCase().endsWith(".xml");
	}

	@Override
	public InputStream preProcess(String path, String method, RemoteClient client, String contentType,
			HttpRequest request, HttpResponse response, InputStream source, File sourceFile) throws IOException {
		// Read manifest
		byte[] xmlB = source.readAllBytes();
		source.close();

		// Decode
		String xml = new String(xmlB, "UTF-8");

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

		// Find URL
		String mediaURL = "http://media.jumpstart.com/";
		JsonObject endpoints = descriptorDef.get("manifestSodEndpoints").getAsJsonObject();
		if (endpoints.has("media.jumpstart.com"))
			mediaURL = endpoints.get("media.jumpstart.com").getAsString();
		if (!mediaURL.endsWith("/"))
			mediaURL += "/";

		// Replace
		xml = xml.replace(mediaURL, "http://" + host + ":16518/sentinelproxy.com/");

		// Return
		return new ByteArrayInputStream(xml.getBytes("UTF-8"));
	}

}
