/**
 * 
 */
package org.mule.egateway.transformers;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreManager;
import org.mule.api.transformer.TransformerException;
import org.mule.egateway.MessageIdUtils;
import org.mule.egateway.email.GatewayEmailMessage;
import org.mule.module.BridgeTableModule;
import org.mule.module.getsatisfaction.getsatisfaction.api.Topic;
import org.mule.module.getsatisfaction.getsatisfaction.api.User;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.email.MailProperties;
import org.mule.transport.email.MailUtils;
import org.mule.transport.email.SmtpConnector;
import org.mule.util.MapUtils;
import org.mule.util.StringUtils;
import org.mule.util.TemplateParser;

/**
 * @author mariano
 *
 */
public class ForumPostToEmailMessage  extends AbstractMessageTransformer implements MuleContextAware
{
	private TemplateParser templateParser = TemplateParser.createMuleStyleParser();
	/**
	 * 
	 */
	public ForumPostToEmailMessage() {
        this.registerSourceType(DataTypeFactory.create(Topic.class));
        //this.setReturnDataType(DataTypeFactory.create(GatewayEmailMessage.class));			
	}

	private User getCommunityUser()
	{
		User user = (User) muleContext.getRegistry().get("communityUser");
		return user;
	}
	
	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			GatewayEmailMessage email = null;
			Topic topic = (Topic) message.getPayload();
			
			if(topic != null)
			{
				String messageId = retrieveFromObjectStore(String.valueOf(topic.getId()));
				if(messageId == null)
				{
					// Case 1: New topic
					email = buildTopicEmail(topic, message, outputEncoding);
					storeToObjectStore(String.valueOf(topic.getId()), email.getId());
				}
			}
			
			return email;
		}
		catch(Exception ex)
		{
			throw new TransformerException(this, ex);
		}
	}

	protected String getTo(String style)
	{
		Map<String, String> mappings = (Map<String, String>) muleContext.getRegistry().get("listMappings");
		
		return mappings.get(style);			
	}

	protected String getSubjectPrefix(String style)
	{
		Map<String, String> mappings = (Map<String, String>) muleContext.getRegistry().get("listMappings");
		
		return mappings.get("subject." + style);			
	}
	
	private GatewayEmailMessage buildTopicEmail(Topic topic, MuleMessage message, String outputEncoding) throws TransformerException
	{
        String endpointAddress = endpoint.getEndpointURI().getAddress();
        SmtpConnector connector = (SmtpConnector) endpoint.getConnector();		
        
        User user = getCommunityUser();
        
        String from = "\"" + topic.getAuthor().getName() + "\" <" + user.getEmail() + ">";
        String to = getTo(topic.getStyle()); //lookupProperty(message, MailProperties.TO_ADDRESSES_PROPERTY, endpointAddress);
        String subject = topic.getSubject(); //getSubjectPrefix(topic.getStyle()) + " " + topic.getSubject();
        String contentType = lookupProperty(message, MailProperties.CONTENT_TYPE_PROPERTY, connector.getContentType());
        String cc = lookupProperty(message, MailProperties.CC_ADDRESSES_PROPERTY, connector.getCcAddresses());
        String bcc = lookupProperty(message, MailProperties.BCC_ADDRESSES_PROPERTY, connector.getBccAddresses());
        String replyTo = lookupProperty(message, MailProperties.REPLY_TO_ADDRESSES_PROPERTY, connector.getReplyToAddresses());
        
        Properties headers = new Properties();
        Properties customHeaders = connector.getCustomHeaders();

        if (customHeaders != null && !customHeaders.isEmpty())
        {
            headers.putAll(customHeaders);
        }

        Properties otherHeaders = message.getOutboundProperty(MailProperties.CUSTOM_HEADERS_MAP_PROPERTY);
        if (otherHeaders != null && !otherHeaders.isEmpty())
        {
                headers.putAll(templateParser.parse(new TemplateParser.TemplateCallback()
                {
                    public Object match(String token)
                    {
                        return muleContext.getRegistry().lookupObject(token);
                    }
                }, otherHeaders));

        }

        if (logger.isDebugEnabled())
        {
            StringBuffer buf = new StringBuffer();
            buf.append("Constructing email using:\n");
            buf.append("To: ").append(to);
            buf.append(", From: ").append(from);
            buf.append(", CC: ").append(cc);
            buf.append(", BCC: ").append(bcc);
            buf.append(", Subject: ").append(subject);
            buf.append(", ReplyTo: ").append(replyTo);
            buf.append(", Content type: ").append(contentType);
            buf.append(", Payload type: ").append(message.getPayload().getClass().getName());
            buf.append(", Custom Headers: ").append(MapUtils.toString(headers, false));
            logger.debug(buf.toString());
        }        
        try
        {
        	GatewayEmailMessage email = new GatewayEmailMessage(((SmtpConnector) endpoint.getConnector()).getSessionDetails(endpoint).getSession(), MessageIdUtils.buildMessageId(topic));
        	email.setTopicId(String.valueOf(topic.getId()));
        	
            email.setRecipients(Message.RecipientType.TO, MailUtils.stringToInternetAddresses(to));

            // sent date
            email.setSentDate(Calendar.getInstance().getTime());

            if (StringUtils.isNotBlank(from))
            {
                email.setFrom(MailUtils.stringToInternetAddresses(from)[0]);
            }

            if (StringUtils.isNotBlank(cc))
            {
                email.setRecipients(Message.RecipientType.CC, MailUtils.stringToInternetAddresses(cc));
            }

            if (StringUtils.isNotBlank(bcc))
            {
                email.setRecipients(Message.RecipientType.BCC, MailUtils.stringToInternetAddresses(bcc));
            }

            if (StringUtils.isNotBlank(replyTo))
            {
                email.setReplyTo(MailUtils.stringToInternetAddresses(replyTo));
            }

            email.setSubject(subject);

            for (Iterator iterator = headers.entrySet().iterator(); iterator.hasNext();)
            {
                Map.Entry entry = (Map.Entry) iterator.next();
                email.setHeader(entry.getKey().toString(), entry.getValue().toString());
            }

            setContent(topic.getContent(), email, contentType, message);

            return email;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
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
		
		if(messageId == null) {
			try
			{
				ObjectStore<String> store = (ObjectStore<String>) ((ObjectStoreManager) muleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER)).getObjectStore("messageIds", true);
				
				return store.retrieve(topicId);
			} 
			catch(Throwable ex)
			{
				return null;
			}
		} else {
			logger.info("Topic id " + topicId + " found in bridge table topicId_messageId");
			return messageId;
		}
	}	
	
	private void storeToBridgeTable(String topicId, String messageId) {
		try {
			logger.info("BT:topicId_messageId[topicId -> messageId] => [" + topicId + "] -> [" + messageId + "]");
			
			BridgeTableModule bt = (BridgeTableModule) muleContext.getRegistry().lookupObject("topicId_messageId");
			
			if(bt.containsKey1(topicId)) {
				bt.updateByKey1(topicId, messageId);
				logger.info("Updated message id " + messageId + "for topic id " + topicId + " to bridge table topicId_messageId");
			} else {
				bt.insert(topicId, messageId);
				logger.info("Inserted message id " + messageId + "for topic id " + topicId + " to bridge table topicId_messageId");
			}
		} catch(Throwable ex) {
			logger.error("Could not save message id " + messageId + "for topic id " + topicId + " to bridge table topicId_messageId: " + ex.getMessage(), ex);
		}
		
	}
	
	private void storeToObjectStore(String topicId, String messageId) throws Exception
	{
		logger.info("messageIds[topicId -> messageId] => [" + topicId + "] -> [" + messageId + "]");
		storeToBridgeTable(topicId, messageId);
		try
		{
			ObjectStore<Serializable> store = (ObjectStore<Serializable>) ((ObjectStoreManager) muleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER)).getObjectStore("messageIds", true);
			
	        try {
	            store.store(topicId, messageId);
	            
	        } catch (ObjectAlreadyExistsException e) {
	        	store.remove(topicId);
	        	store.store(topicId, messageId);
	        }
		} 
		catch(Exception ex)
		{
			throw ex;
		}
	}
	
	  /**
     * Searches in outbound, then invocation scope. If not found, returns a passed in default value.
     */
    protected String lookupProperty(MuleMessage message, String propName, String defaultValue)
    {
        String value = message.getOutboundProperty(propName);
        if (value == null)
        {
            value = message.getInvocationProperty(propName, defaultValue);
        }
        return evaluate(value, message);
    }

    public String evaluate(String value, MuleMessage message)
    {
        if(value != null && muleContext.getExpressionManager().isExpression(value))
        {
            value = (String) muleContext.getExpressionManager().evaluate(value, message);
        }
        return value;
    }

    protected void setContent(Object payload, Message msg, String contentType, MuleMessage message)
        throws Exception
    {
        msg.setContent(payload, contentType);
    }		
}
