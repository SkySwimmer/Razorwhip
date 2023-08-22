package org.asf.razorwhip.sentinel.launcher.descriptors.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

public class ContentDownloaderStream extends InputStream {

	private URLConnection conn;

	public ContentDownloaderStream(URLConnection conn) {
		this.conn = conn;
	}

	@Override
	public int read() throws IOException {
		return conn.getInputStream().read();
	}

	@Override
	public int read(byte b[], int off, int len) throws IOException {
		return conn.getInputStream().read(b, off, len);
	}

	@Override
	public byte[] readNBytes(int len) throws IOException {
		return conn.getInputStream().readNBytes(len);
	}

	@Override
	public void close() throws IOException {
		conn.getInputStream().close();
	}

}
