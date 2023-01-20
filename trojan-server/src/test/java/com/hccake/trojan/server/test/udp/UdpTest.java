package com.hccake.trojan.server.test.udp;

import org.junit.jupiter.api.Test;
import sockslib.client.Socks5;
import sockslib.client.Socks5DatagramSocket;
import sockslib.client.SocksProxy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author hccake
 */
class UdpTest {

	@Test
	void testUdp() throws IOException {
		SocksProxy proxy = new Socks5(new InetSocketAddress("127.0.0.1", 7890)); // clash 代理地址
		// proxy.setCredentials(new UsernamePasswordCredentials(username, password));
		DatagramSocket socket = new Socks5DatagramSocket(proxy);

		byte[] receiveData = new byte[1024];
		byte[] sendData = "Hello,Test".getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
				InetAddress.getByName("127.0.0.1"), UdpEchoServer.PORT);
		socket.send(sendPacket);
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		socket.receive(receivePacket);
		String modifiedSentence = new String(receivePacket.getData());
		System.out.println("FROM SERVER 1:" + modifiedSentence);

		socket.send(sendPacket);
		DatagramPacket receivePacket2 = new DatagramPacket(receiveData, receiveData.length);
		socket.receive(receivePacket2);
		String modifiedSentence2 = new String(receivePacket2.getData());
		System.out.println("FROM SERVER 2:" + modifiedSentence2);

		socket.close();
	}

}
