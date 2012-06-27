/**
 * 
 */
package org.mule.egateway.transformers;

import java.util.ArrayList;
import java.util.List;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.module.getsatisfaction.getsatisfaction.api.Reply;
import org.mule.module.getsatisfaction.getsatisfaction.api.Topic;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;

/**
 * @author mariano
 *
 */
public class AllRepliesToNewReplies extends AbstractMessageTransformer {

	/**
	 * 
	 */
	public AllRepliesToNewReplies() {
		this.registerSourceType(DataTypeFactory.create(Topic.class));
	}

	/* (non-Javadoc)
	 * @see org.mule.transformer.AbstractMessageTransformer#transformMessage(org.mule.api.MuleMessage, java.lang.String)
	 */
	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
		List<Reply> repliesTosend = new ArrayList<Reply>();
		List<Reply> replies = (List<Reply>) message.getInvocationProperty("replies");
		if(replies != null)
		{
			Topic topic = (Topic) message.getPayload();
			long lastUpdate = Long.valueOf((String) message.getInvocationProperty("lastPoll") ) * 1000L;
			
			
			for(Reply reply : replies)
			{
				if(reply != null && reply.getCreatedAt() != null && reply.getCreatedAt().getTime() >= lastUpdate)
				{
					repliesTosend.add(reply);
				}
			}
			message.setProperty("topic", topic, PropertyScope.INVOCATION);
		}
		
		message.removeProperty("replies", PropertyScope.INVOCATION);
		return repliesTosend;
	}

}
