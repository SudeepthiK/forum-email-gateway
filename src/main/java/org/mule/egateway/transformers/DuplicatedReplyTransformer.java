/**
 * 
 */
package org.mule.egateway.transformers;

import java.io.Serializable;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;
import org.mule.api.transformer.TransformerException;
import org.mule.module.BridgeTableModule;
import org.mule.module.getsatisfaction.getsatisfaction.api.Reply;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.TemplateParser;

/**
 * @author mariano
 *
 */
public class DuplicatedReplyTransformer  extends AbstractMessageTransformer implements MuleContextAware
{
	private TemplateParser templateParser = TemplateParser.createMuleStyleParser();
	/**
	 * 
	 */
	public DuplicatedReplyTransformer() {
        this.registerSourceType(DataTypeFactory.create(Reply.class));
        //this.setReturnDataType(DataTypeFactory.create(GatewayEmailMessage.class));			
	}


	
	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			Reply reply = (Reply) message.getPayload();
			if(reply != null)
			{
				String messageId = retrieveFromObjectStore(String.valueOf(reply.getId()));
				if(messageId == null)
				{
					return reply;
				}
			}
			return null;
		}
		catch(Exception ex)
		{
			throw new TransformerException(this, ex);
		}
	}

	private String retrieveFromBridgeTable(String replyId)
	{
		try {
			BridgeTableModule bt = (BridgeTableModule) muleContext.getRegistry().lookupObject("replyId_messageId");
			return bt.retrieveByKey1(replyId).toString();
		} catch(Throwable ex) {
			logger.debug("Could not retrieve message id for reply id " + replyId + " from bridge table replyId_messageId: " + ex.getMessage(), ex);
			return null;
		}

	}
	
	private String retrieveFromObjectStore(String replyId)
	{
		String messageId = retrieveFromBridgeTable(replyId);
		
		if(messageId == null)
		{
			try
			{
				ObjectStore<Serializable> store = (ObjectStore<Serializable>) ((ObjectStoreManager) muleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER)).getObjectStore("replyMessageIds", true);
				
				return store.retrieve(replyId).toString();
			} 
			catch(ObjectStoreException ex)
			{
				logger.debug("Reply id " + replyId + " not found in object store replyMessageIds");
				return null;
			}
			catch(Throwable ex)
			{
				logger.error("Error retrieving Reply id " + replyId + " from object store replyMessageIds: " + ex.getMessage(), ex);
				return null;
			}			
		} else {
			logger.info("Reply id " + replyId + " found in bridge table replyId_messageId");
			return messageId;
		}

	}	
	

}
