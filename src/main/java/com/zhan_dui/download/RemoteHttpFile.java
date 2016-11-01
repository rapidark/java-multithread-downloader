package com.zhan_dui.download;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class RemoteHttpFile {

	BufferedInputStream bufferedInputStream = null;
	
	public RemoteHttpFile(String fileUrl, int currentPosition, int endPosition) throws IOException {
		URL url = new URL(fileUrl);
		URLConnection urlConnection = url.openConnection();
		urlConnection.setRequestProperty("Range", "bytes=" + currentPosition + "-" + endPosition);
		bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
	}

	public int read(byte[] buf) throws IOException {
		return bufferedInputStream.read(buf, 0, buf.length);
	}

	public void close() throws IOException {
		bufferedInputStream.close();
	}

}
