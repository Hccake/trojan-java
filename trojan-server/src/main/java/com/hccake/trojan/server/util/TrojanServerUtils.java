package com.hccake.trojan.server.util;

import io.netty5.channel.Channel;
import io.netty5.channel.ChannelFutureListeners;

/**
 * @author hccake
 */
public final class TrojanServerUtils {

	private TrojanServerUtils() {
	}

	/**
	 * Closes the specified channel after all queued write requests are flushed.
	 */
	public static void closeOnFlush(Channel ch) {
		if (ch.isActive()) {
			ch.writeAndFlush(ch.bufferAllocator().allocate(0)).addListener(ch, ChannelFutureListeners.CLOSE);
		}
	}
}
