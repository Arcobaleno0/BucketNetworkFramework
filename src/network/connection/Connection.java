package network.connection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

import Common.Tool;
import network.command.BucketCommand;
import network.listener.BucketListener;

public class Connection {

	public Socket socket;

	private String encoding;
	private BucketListener listener;

	private boolean quit;

	protected BufferedOutputStream out;
	protected BufferedInputStream in;

	public Connection(Socket socket, BucketListener messageListener) throws IOException {
		encoding = "GBK";
		this.socket = socket;
		this.listener = messageListener;
		this.out = (new BufferedOutputStream(socket.getOutputStream()));
		this.in = (new BufferedInputStream(socket.getInputStream()));
//		this.out = socket.getOutputStream();
//		this.in = socket.getInputStream();
		this.quit = false;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getEncoding() {
		return encoding;
	}

	public void send(String message) throws IOException {
		int length = message.getBytes(getEncoding()).length;
		byte[] b = (length + "\n" + message).getBytes(getEncoding());
		out.write(b);
		out.flush();
	}

	public void send(BucketCommand message) throws IOException {
		send(Tool.toJson(message));
	}

	public void setListener(BucketListener listener) {
		this.listener = listener;
	}

	public BucketListener getListener() {
		return listener;
	}

	public void stopListen() {
		this.quit = true;
	}
	
	public void finish() throws IOException
	{
		stopListen();
		in.close();
		out.close();
		socket.close();
	}

	public void startListen() throws IOException {
		String lenStr;

		while ((lenStr = readLine()) != "EOF") {
			int len;
			try {
				len = Integer.valueOf(lenStr);
				ByteArrayOutputStream o = new ByteArrayOutputStream(len);

				byte[] data = new byte[len];
				in.read(data,0,len);
				int b = 0;
				
				for(int i = 0;i < len; i++)
				{
					b = in.read();
					if(b == -1)
						break;
					o.write(b);
				}
				
				o.flush();

				if (listener != null)
					listener.onDataCome(this, new String(o.toByteArray(), getEncoding()));

			} catch (NumberFormatException e) {

			}
		}

		if (listener != null)
			listener.onDisconnection(this);

	}

	protected String readLine() throws IOException {
		ByteArrayOutputStream str = new ByteArrayOutputStream();

		int b;
		try {
			while ((b = in.read()) != -1 && !this.quit) {
				if ((char) b == '\n') {
					str.flush();
					return new String(str.toByteArray(), getEncoding());
				}
				str.write(b);

			}
		} catch (java.net.SocketException | java.net.SocketTimeoutException e) {
			finish();
		}

		return "EOF";

	}

	protected void writeLine(String str) throws UnsupportedEncodingException, IOException {
		out.write((str + "\n").getBytes(getEncoding()));
		out.flush();
	}

}
