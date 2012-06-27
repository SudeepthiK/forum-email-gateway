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
import org.mule.module.getsatisfaction.getsatisfaction.api.Topic;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.TemplateParser;

/**
 * @author mariano
 *
 */
public class DuplicatedTopicTransformer  extends AbstractMessageTransformer implements MuleContextAware
{
	private TemplateParser templateParser = TemplateParser.createMuleStyleParser();
	/**
	 * 
	 */
	public DuplicatedTopicTransformer() {
        this.registerSourceType(DataTypeFactory.create(Topic.class));
        //this.setReturnDataType(DataTypeFactory.create(GatewayEmailMessage.class));			
	}
	
	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			Topic topic = (Topic) message.getPayload();
			
			if(topic != null)
			{
				String messageId = retrieveFromObjectStore(String.valueOf(topic.getId()));
				if(messageId == null)
				{
					return topic;
				}
			}
			
			
			return null;
		}
		catch(Exception ex)
		{
			throw new TransformerException(this, ex);
		}
	}

	private String retrieveFromBridgeTable(String topicId)
	{
		try {
			BridgeTableModule bt = (BridgeTableModule) muleContext.getRegistry().lookupObject("topicId_messageId");
			return bt.retrieveByKey1(topicId).toString();
		} catch(Throwable ex) {
			logger.error("Could not retrieve message id for topic id " + topicId + " from bridge table replyId_messageId: " + ex.getMessage(), ex);
			return null;
		}

	}
	
	private String retrieveFromObjectStore(String topicId)
	{
		String messageId = retrieveFromBridgeTable(topicId);
		
		if(messageId == null)
		{		
			try
			{
				ObjectStore<Serializable> store = (ObjectStore<Serializable>) ((ObjectStoreManager) muleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER)).getObjectStore("messageIds", true);
				
				return store.retrieve(topicId).toString();
			} 
			catch(ObjectStoreException ex)
			{
				logger.info("Topic id " + topicId + " not found in object store messageIds");
				return null;
			}
			catch(Throwable ex)
			{
				logger.error("Error retrieving Topic id " + topicId + " from object store messageIds: " + ex.getMessage(), ex);
				return null;
			}
		} else {
			logger.info("Topic id " + topicId + " found in bridget table topicId_messageId");
			return messageId;			
		}
	}	
	
	
}
