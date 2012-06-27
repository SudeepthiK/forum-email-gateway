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
import org.mule.module.getsatisfaction.getsatisfaction.api.Reply;
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
public class ForumReplyToEmailMessage  extends AbstractMessageTransformer implements MuleContextAware
{
	private TemplateParser templateParser = TemplateParser.createMuleStyleParser();
	/**
	 * 
	 */
	public ForumReplyToEmailMessage() {
        this.registerSourceType(DataTypeFactory.create(Reply.class));
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
			Reply reply = (Reply) message.getPayload();
			Topic topic = (Topic) message.getInvocationProperty("topic");
			if(reply != null)
			{
				String messageId = retrieveFromObjectStore(String.valueOf(reply.getId()));
				if(messageId == null)
				{
					// Case 1: New reply
					email = buildReplyEmail(reply, topic, message, outputEncoding);
					storeToObjectStore(String.valueOf(reply.getId()), email.getId());
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
	
	private GatewayEmailMessage buildReplyEmail(Reply reply, Topic topic, MuleMessage message, String outputEncoding) throws TransformerException
	{
        String endpointAddress = endpoint.getEndpointURI().getAddress();
        SmtpConnector connector = (SmtpConnector) endpoint.getConnector();		
        
        User user = getCommunityUser();
        
        String from = "\"" + reply.getAuthor().getName() + "\" <" + user.getEmail() + ">";
        String to = getTo(topic.getStyle()); //lookupProperty(message, MailProperties.TO_ADDRESSES_PROPERTY, endpointAddress);
        String subject = "RE: " + topic.getSubject();
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
        //headers.put("In-Reply-To", topic.getId());
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

            setContent(reply.getContent(), email, contentType, message);

            return email;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }        
	}
	
	
	private String retrieveFromBridgeTable(String replyId)
	{
		try {
			BridgeTableModule bt = (BridgeTableModule) muleContext.getRegistry().lookupObject("replyId_messageId");
			return bt.retrieveByKey1(replyId).toString();
		} catch(Throwable ex) {
			logger.info("Could not retrieve message id for reply id " + replyId + " from bridge table replyId_messageId: " + ex.getMessage(), ex);
			return null;
		}

	}	
	
	private void storeToBridgeTable(String replyId, String messageId) {
		try {
			logger.info("BT:replyId_messageId[topicId -> messageId] => [" + replyId + "] -> [" + messageId + "]");
			BridgeTableModule bt = (BridgeTableModule) muleContext.getRegistry().lookupObject("replyId_messageId");
			
			if(bt.containsKey1(replyId)) {
				bt.updateByKey1(replyId, messageId);
				logger.info("Updated message id " + messageId + "for reply id " + replyId + " to bridge table replyId_messageId");
			} else {
				bt.insert(replyId, messageId);
				logger.info("Inserted message id " + messageId + "for reply id " + replyId + " to bridge table replyId_messageId");
			}
		} catch(Throwable ex) {
			logger.error("Could not save message id " + messageId + "for reply id " + replyId + " to bridge table replyId_messageId: " + ex.getMessage(), ex);
		}
		
	}
	
	private String retrieveFromObjectStore(String replyId)
	{
		String messageId = retrieveFromBridgeTable(replyId);
		
		if(messageId == null) {		
			try
			{
				ObjectStore<String> store = (ObjectStore<String>) ((ObjectStoreManager) muleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER)).getObjectStore("replyMessageIds", true);
				
				return store.retrieve(replyId);
			} 
			catch(Throwable ex)
			{
				return null;
			}
		} else {
			logger.info("Reply id " + replyId + " found in bridge table replyId_messageId");
			return messageId;
		}
	}	
	
	private void storeToObjectStore(String replyId, String messageId) throws Exception
	{
		logger.info("replyMessageIds[topicId -> messageId] => [" + replyId + "] -> [" + messageId + "]");
		storeToBridgeTable(replyId, messageId);
		try
		{
			ObjectStore<Serializable> store = (ObjectStore<Serializable>) ((ObjectStoreManager) muleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER)).getObjectStore("replyMessageIds", true);
			
	        try {
	            store.store(replyId, messageId);
	        } catch (ObjectAlreadyExistsException e) {
	        	store.remove(replyId);
	        	store.store(replyId, messageId);
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
