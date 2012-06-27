/**
 * 
 */
package org.mule.egateway.email;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * @author mariano
 *
 */
public class GatewayEmailMessage extends MimeMessage {
	public static String MESSAGE_ID_HEADER = "Message-ID";
	
	private String id;
	private String topicId;
	
	public String getTopicId() {
		return topicId;
	}

	public void setTopicId(String topicId) {
		this.topicId = topicId;
	}

	/**
	 * @param session
	 */
	public GatewayEmailMessage(Session session, String id) {
		super(session);
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	protected void updateMessageID() throws MessagingException {
		setHeader(MESSAGE_ID_HEADER, getId());
	}	

}
