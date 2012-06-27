/**
 * 
 */
package org.mule.egateway;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.UUID;

import org.mule.module.getsatisfaction.getsatisfaction.api.Reply;
import org.mule.module.getsatisfaction.getsatisfaction.api.Topic;

/**
 * @author mariano
 *
 */
public class MessageIdUtils {

	/**
	 * 
	 */
	public MessageIdUtils() {
	}

	public static boolean isValidMessageId(String messageId)
	{
		return messageId != null && messageId.trim().startsWith("<topic.");
	}
	
	public static String buildMessageId(Topic topic) {

		StringBuilder id = new StringBuilder();
		
		id.append("<topic.");
		id.append(topic.getId());
		id.append(".");
		id.append(UUID.randomUUID().toString());
		id.append("@");
		id.append(getHostName());
		id.append(">");	
		
		return id.toString();
	}

	public static String buildMessageIdX(Reply reply) {

		StringBuilder id = new StringBuilder();
		
		id.append("<reply.");
		id.append(reply.getId());
		id.append(".");
		id.append(UUID.randomUUID().toString());
		id.append("@");
		id.append(getHostName());
		id.append(">");	
		
		return id.toString();
	}
	
	public static long getTopicId(String messageId) {
		if(isValidMessageId(messageId)) {
			StringTokenizer st = new StringTokenizer(messageId, ".");
			if(st.hasMoreTokens()) {
				st.nextToken(); // jumping over <topic.
			}
			return Long.parseLong(st.nextToken());
		}
		else
		{
			return -1L;
		}
	}
	
	
	private static String getHostName() {
		try {
		    InetAddress addr = InetAddress.getLocalHost();

		    // Get hostname
		    String hostname = addr.getCanonicalHostName();
		    return hostname;
		} catch (UnknownHostException e) {
			return "gateway.mulesoft.org";
		}	
	}	

}
