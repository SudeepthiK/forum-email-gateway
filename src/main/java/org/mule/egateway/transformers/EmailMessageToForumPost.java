/**
 * 
 */
package org.mule.egateway.transformers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang.StringEscapeUtils;
import org.mortbay.log.Log;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;
import org.mule.api.transformer.TransformerException;
import org.mule.egateway.MessageIdUtils;
import org.mule.module.BridgeTableModule;
import org.mule.module.getsatisfaction.getsatisfaction.api.Post;
import org.mule.module.getsatisfaction.getsatisfaction.api.Reply;
import org.mule.module.getsatisfaction.getsatisfaction.api.Topic;
import org.mule.module.getsatisfaction.getsatisfaction.api.TopicStyle;
import org.mule.module.getsatisfaction.getsatisfaction.api.User;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;

/**
 * @author mariano
 *
 */
public class EmailMessageToForumPost extends AbstractMessageTransformer implements MuleContextAware {
	private static String LIST_FOOTER_LINE_1 = "---------------------------------------------------------------------";
	private static String LIST_FOOTER_LINE_2 = "To unsubscribe from this list, please visit:";
	private static String LIST_FOOTER_LINE_3 = "http://xircles.codehaus.org/manage_email";
	private static String NEW_LINE = "\n";
	//private static String CODE_START = "<code>";
	//private static String CODE_END = "</code>";
	//private static String HTML_NEW_LINE = "<br />";
	
	/**
	 * 
	 */
	public EmailMessageToForumPost() {
        this.registerSourceType(DataTypeFactory.create(String.class));
        //this.registerSourceType(DataTypeFactory.create(Message.class));
	}

	public static void main(String args[]) {
		String a = "Hi\n<br /><br />\nThis is an example of how to do a query:\n<br /><br />\n<code>\n<br />\n&lt;?xml version=&quot;1&#46;0&quot; encoding=&quot;UTF-8&quot;?&gt;\n<br />\n&lt;mule xmlns=&quot;http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;core&quot;\n<br />\n     xmlns:xsi=&quot;http:&#47;&#47;www&#46;w3&#46;org&#47;2001&#47;XMLSchema-instance&quot;\n<br />\n     xmlns:spring=&quot;http:&#47;&#47;www&#46;springframework&#46;org&#47;schema&#47;beans&quot;\n<br />\n     xmlns:file=&quot;http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;file&quot;\n<br />\n     xmlns:vm=&quot;http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;vm&quot;\n<br />\n     xmlns:scripting=&quot;http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;scripting&quot;\n<br />\n     xmlns:jdbc=&quot;http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;jdbc&quot;\n<br />\n     xmlns:http=&quot;http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;http&quot;\n<br />\n     xmlns:script=&quot;http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;scripting&quot;\n<br />\n   xsi:schemaLocation=&quot;\n<br />\n       http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;core http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;core&#47;3&#46;2&#47;mule&#46;xsd\n<br />\n       http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;file http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;file&#47;3&#46;2&#47;mule-file&#46;xsd\n<br />\n       http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;vm http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;vm&#47;3&#46;2&#47;mule-vm&#46;xsd\n<br />\n       http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;scripting http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;scripting&#47;3&#46;2&#47;mule-scripting&#46;xsd\n<br />\n       http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;jdbc http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;jdbc&#47;3&#46;2&#47;mule-jdbc&#46;xsd\n<br />\n       http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;http http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;http&#47;3&#46;2&#47;mule-http&#46;xsd\n<br />\n       http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;scripting http:&#47;&#47;www&#46;mulesoft&#46;org&#47;schema&#47;mule&#47;scripting&#47;3&#46;2&#47;mule-scripting&#46;xsd\n<br />\n       http:&#47;&#47;www&#46;springframework&#46;org&#47;schema&#47;beans http:&#47;&#47;www&#46;springframework&#46;org&#47;schema&#47;beans&#47;spring-beans-3&#46;0&#46;xsd&quot;&gt;\n<br /><br />\n       &lt;!-- Configuration for Metadata Store --&gt;\n<br />\n   &lt;spring:bean id=&quot;jdbcProperties&quot; class=&quot;org&#46;springframework&#46;beans&#46;factory&#46;config&#46;PropertyPlaceholderConfigurer&quot;&gt;\n<br />\n       &lt;spring:property name=&quot;location&quot; value=&quot;classpath:jdbc&#46;properties&quot;&#47;&gt;\n<br />\n   &lt;&#47;spring:bean&gt;\n<br /><br />\n       &lt;!-- Metadata Store configuration --&gt;\n<br />\n   &lt;spring:bean id=&quot;jdbcDataSource&quot;\n<br />\n       class=&quot;org&#46;enhydra&#46;jdbc&#46;standard&#46;StandardDataSource&quot;\n<br />\n       destroy-method=&quot;shutdown&quot;&gt;\n<br />\n       &lt;spring:property name=&quot;driverName&quot; value=&quot;${database&#46;driver}&quot;&#47;&gt;\n<br />\n       &lt;spring:property name=&quot;url&quot; value=&quot;${database&#46;connection}&quot;&#47;&gt;\n<br />\n   &lt;&#47;spring:bean&gt;\n<br /><br />\n   &lt;jdbc:connector name=&quot;jdbcConnector&quot; dataSource-ref=&quot;jdbcDataSource&quot; queryTimeout=&quot;1000&quot;&gt;\n<br />\n       &lt;jdbc:query key=&quot;insertMetadata&quot; value=&quot;insert into sap_idoc_metadata (name, metadata) values (#[header:OUTBOUND:filename], #[payload:])&quot;&#47;&gt;\n<br />\n   &lt;&#47;jdbc:connector&gt;\n<br /><br />\n   &lt;flow name=&quot;JDBC Flow name&quot;&gt;\n<br />\n   &#46;&#46;&#46;\n<br /><br />\n               &lt;jdbc:outbound-endpoint queryKey=&quot;insertMetadata&quot; exchange-pattern=&quot;one-way&quot; connector-ref=&quot;jdbcConnector&quot; queryTimeout=&quot;10&quot; &gt;\n<br />\n                   &lt;jdbc:transaction action=&quot;ALWAYS_BEGIN&quot;&#47;&gt;\n<br />\n               &lt;&#47;jdbc:outbound-endpoint&gt;\n<br />\n   &lt;&#47;flow&gt;\n<br /><br />\n&lt;&#47;mule&gt;\n<br /><br />\n</code>\n<br /><br />\nCheck that:\n<br />\n1) JDBC configuration properties are in a file, but you can replace ${...} with your values\n<br />\n2) You need to create a JDBC DataSource\n<br />\n3) Create the JDBC Connector\n<br />\n4) Use the JDBC outbound endpoint in your code (The transaction is not needed if you don't want transactions)";
		System.out.println(cleanUpMailContents(a));
	}
	
	private static String cleanUpMailContents(String contents) {
		StringBuilder sb = new StringBuilder();
		
		BufferedReader reader = new BufferedReader(new StringReader(contents));
		String str;
		boolean foundLine1 = false;
		boolean stop = false;
		boolean inCode = false;
		
		try {
		  while ((str = reader.readLine()) != null) {
			  //str = StringUtils.replace(str, HTML_NEW_LINE, "");
			  if(LIST_FOOTER_LINE_1.equalsIgnoreCase(str)) {
				  foundLine1 = true;
			  } else if(LIST_FOOTER_LINE_2.equalsIgnoreCase(str) && foundLine1) {
				  stop = true;
			  } else if(!LIST_FOOTER_LINE_2.equalsIgnoreCase(str) && foundLine1) {
				  sb.append(LIST_FOOTER_LINE_1);
				  sb.append(NEW_LINE);
				  foundLine1 = false;
			  } else if(LIST_FOOTER_LINE_2.equalsIgnoreCase(str) && !foundLine1 && !stop) {
				  sb.append(LIST_FOOTER_LINE_1);
				  sb.append(NEW_LINE);
				  stop = true;
			  } else if(!stop && (str.contains(LIST_FOOTER_LINE_2) || str.contains(LIST_FOOTER_LINE_3))) {
				  // Ignoring lines with this content
			  } else if(!stop && str.trim().startsWith(">")) {
				  // Ignoring answer lines
			  } else if(!stop) {
				  sb.append(inCode ? StringEscapeUtils.unescapeHtml(str): str);
				  sb.append(NEW_LINE);
				  /*
				  if(str.toLowerCase().contains(CODE_START) && !inCode) {
					  inCode = true;
				  }
				  sb.append(inCode ? StringEscapeUtils.unescapeHtml(str): str);
				  sb.append(NEW_LINE);
				  if(str.toLowerCase().contains(CODE_END) && inCode) {
					  inCode = false;
				  }	
				  */			  
			  }
		  }

		} catch(IOException e) {
		  return contents;
		}		
		
		return sb.toString();
	}
	
	private User getCommunityUser()
	{
		User user = (User) muleContext.getRegistry().get("communityUser");
		return user;
	}
	
	protected Map<String, String> getListTopicStyleMappings()
	{
		Map<String, String> mappings = (Map<String, String>) muleContext.getRegistry().get("listMappings");
		return mappings;		
	}
	
	protected String getStyle(String to)
	{
		Map<String, String> mappings = getListTopicStyleMappings();
		for(Entry<String, String> e : mappings.entrySet())
		{
			if(to.contains(e.getValue()))
			{
				return e.getKey();
			}
		}
		return TopicStyle.QUESTION.toString();
	}
	
	/**
	 * @see org.mule.transformer.AbstractMessageTransformer#transformMessage(org.mule.api.MuleMessage, java.lang.String)
	 */
	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
		Map<String, String> mappings = getListTopicStyleMappings();
		
		if(!sentToList(message, mappings.values()))
		{
			logger.info("The original message was not sent to any of " + mappings.values() + " . Message " + message);
			return null;
		}
		
		Object o = message.getInvocationProperty("ldapUser");
		Map<String, Object> entry = o != null && !(o instanceof NullPayload) ? (Map<String, Object>) o : null;
		
		User communityUser = getCommunityUser();
		User user;
		if(entry != null)
		{
			user = new User();
			user.setEmail(getFromAddress(message));
			user.setFullName((String) entry.get("cn"));
			user.setUid((String) entry.get("uid"));
		}
		else
		{
			logger.warn("No user in LDAP match email address [" + EmailMessageToForumPost.getFromAddress(message) + "] using community user");
			user = communityUser;
		}
		return createPost(createMailMessage(message, mappings), user);
	}

	private Post createPost(MailMessage message, User user)
	{
		String content = cleanUpMailContents(message.getContent());
		
		// Ignore valid message ids OR messages that come from the community user
		if(MessageIdUtils.isValidMessageId(message.getInReplyTo()))
		{
			// Case 1: Reply to message created in the Forum
			Reply reply = new Reply();
			reply.setTopicId(MessageIdUtils.getTopicId(message.getInReplyTo()));
			reply.setUser(user);
			reply.setContent(content);
			reply.setMessageId(message.getMessageId());

			logger.info("Case 1: In reply to [" + message.getInReplyTo() + "] Replying to topic [" + reply.getTopicId() + "]");

			
			return reply;
		}
		else if(message.getInReplyTo() == null || message.getInReplyTo().trim().length() <= 0)
		{
			logger.info("Case 2: New Topic from Mail List");
			
			// Case 2: New Topic
			Topic topic = new Topic();
			topic.setUser(user);
			topic.setSubject(message.getSubject());
			topic.setContent(content);
			topic.setMessageId(message.getMessageId());
			if(message.getTo() != null)
			{
				topic.setStyle(getStyle(message.getTo().getAddress()));
			}
			return topic;
		}
		else
		{
			Long topicId = null;
			List<String> msgIds = new ArrayList<String>();
			msgIds.add(message.getInReplyTo());
			msgIds.addAll(message.getReferences());

			// Case 3: Reply from the list to topic created in the Forum
			for(String messageId : msgIds)
			{
				if(MessageIdUtils.isValidMessageId(messageId))
				{
					topicId = new Long(MessageIdUtils.getTopicId(messageId));
					logger.info("Case 3: Reply from list to topic created in the forum [" + message.getInReplyTo() + "] Replying to topic [" + topicId + "]");
					
				}
			}

			if(topicId == null)
			{
				// Case 4: Reply to message created in the List
				for(String messageId : msgIds)
				{
					topicId = retrieveFromObjectStore(messageId);
					if(topicId != null) {
						logger.info("Case 4: Reply to message created in the list. MessageId [" + messageId + "] matched Topic id [" + topicId + "]");
						break;
					}
				}
			}
			
			if(topicId != null)
			{
				Reply reply = new Reply();
				reply.setTopicId(topicId.longValue());
				reply.setUser(user);
				reply.setContent(content);
				reply.setMessageId(message.getMessageId());
				return reply;				
			}
			else
			{
				// Case 5: Reply to email that is not a topic?
				Log.warn("Reply to email that is not a topic");
			}
		}
		
		return null;
	}
	
	public static String getFromAddress(MuleMessage message)
	{
		InternetAddress from = null;
		try
		{
			
			if(message.getPayload() instanceof String)
			{
				from = new InternetAddress((String) message.getOutboundProperty("From"));
			}
			else if(message.getPayload() instanceof Message)
			{
				Message email = (Message) message.getPayload();
				Address[] froms = email.getFrom();
				from = froms != null && froms.length > 0 ? (InternetAddress) froms[0] : null;				
			}
			return from != null ? from.getAddress() : null;
		}
		catch(Throwable ex)
		{
			return null;
		}	
	}
	
	public InternetAddress getToListAddress(Address[] tos, Collection<String> listAddresses)
	{
		for(Address a : tos)
		{
			for(String listAddress: listAddresses)
			{
				if(a instanceof InternetAddress && ((InternetAddress) a).getAddress().contains(listAddress))
				{
					return (InternetAddress) a;
				}
			}
		}		
		return null;
	}
	
	public InternetAddress getToListAddress(String to, Collection<String> listAddresses)
	{
		try
		{
			for(String address : listAddresses)
			{
				if(to.contains(address))
				{
					return new InternetAddress(address);
				}
			}
			return null;
		}
		catch(MessagingException ex)
		{
			return null;
		}
	}
	
	public boolean sentToList(MuleMessage message, Collection<String> listAddresses)
	{
		try
		{
			
			if(message.getPayload() instanceof String)
			{
				Object toObj = message.getOutboundProperty("To");
				if(toObj instanceof String)
				{
					for(String listAddress: listAddresses)
					{
						if(toObj.toString().contains(listAddress))
						{
							return true;
						}
						
					}
				}
				else if(toObj instanceof Collection)
				{
					for(Object o : (Collection) toObj)
					{
						for(String listAddress: listAddresses)
						{
							if(o.toString().contains(listAddress))
							{
								return true;
							}
						}
					}
				}
			}
			else if(message.getPayload() instanceof Message)
			{
				Message email = (Message) message.getPayload();
				Address[] tos = email.getAllRecipients();
				for(Address a : tos)
				{
					for(String listAddress: listAddresses)
					{
						if(a instanceof InternetAddress && ((InternetAddress) a).getAddress().contains(listAddress))
						{
							return true;
						}
					}
				}
			}
			return false;
		}
		catch(Throwable ex)
		{
			return false;
		}	
	}

	private Long retrieveFromBridgeTable(String messageId)
	{
		try {
			BridgeTableModule bt = (BridgeTableModule) muleContext.getRegistry().lookupObject("topicId_messageId");
			return Long.parseLong(bt.retrieveByKey2(messageId).toString());
		} catch(Throwable ex) {
			logger.error("Could not retrieve topic id for message id " + messageId + " from bridge table topicId_messageId: " + ex.getMessage(), ex);
			return null;
		}

	}
	
	private Long retrieveFromObjectStore(String messageId)
	{
		logger.info("topicIds -> Searching for messageId: [" + messageId + "]");
		Long id = retrieveFromBridgeTable(messageId);
		
		if(id == null)
		{
			try
			{
				ObjectStore<Serializable> store = (ObjectStore<Serializable>) ((ObjectStoreManager) muleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER)).getObjectStore("topicIds", true);
				
				return Long.parseLong(store.retrieve(messageId).toString());
			} 
			catch(ObjectStoreException ex)
			{
				logger.info("Message id " + messageId + " not found in object store topicIds");
				return null;
			}
			catch(Throwable ex)
			{
				logger.error("Error retrieving Message id " + messageId + " from object store topicIds: " + ex.getMessage(), ex);
				return null;
			}
		} else {
			logger.info("Message id " + messageId + " found in bridget table topicId_messageId");
			return id;
		}
	}
	
	private MailMessage createMailMessage(MuleMessage message, Map<String, String> mappings) throws TransformerException
	{
		try
		{
			MailMessage msg = null;
			if(message.getPayload() instanceof String) {
				msg = new MailMessage();
				
				msg.setMessageId((String) message.getOutboundProperty("Message-ID"));
				msg.setInReplyTo((String) message.getOutboundProperty("In-Reply-To"));
				msg.setReferences((String) message.getOutboundProperty("References"));
				// Parse references
				msg.setFrom(new InternetAddress((String) message.getOutboundProperty("From")));
//				msg.setTo(new InternetAddress((String) message.getOutboundProperty("To")));
				msg.setTo(getToListAddress((String) message.getOutboundProperty("To"), mappings.values()));
				msg.setSubject((String) message.getOutboundProperty("Subject"));
				msg.setContent((String) message.getPayload());
			} else if(message.getPayload() instanceof Message) {
				msg = new MailMessage();
				Message email = (Message) message.getPayload();
				
				msg.setMessageId(email.getHeader("Message-ID")[0]);
				msg.setInReplyTo(email.getHeader("In-Reply-To")[0]);
				msg.setReferences(email.getHeader("References"));
				Address[] froms = email.getFrom();
				InternetAddress from = froms != null && froms.length > 0 ? (InternetAddress) froms[0] : null;
				msg.setFrom(from);
				Address[] tos = email.getFrom();
				//InternetAddress to = tos != null && tos.length > 0 ? (InternetAddress) tos[0] : null;
				msg.setTo(getToListAddress(tos, mappings.values()));
				//msg.setTo(new InternetAddress(listAddress));
				msg.setSubject(email.getSubject());
				msg.setContent(getMessageBody(email));
			}
			return msg;
		} catch(Exception e) {
			throw new TransformerException(this, e);
		}			
		
	}

	private String getMessageBody(Message msg) throws MessagingException, IOException {
        Object result = msg.getContent();
        if (result instanceof String)
        {
            return (String) result;
        }
        else if (result instanceof MimeMultipart)
        {
            // very simplistic, only gets first part
            BodyPart firstBodyPart = ((MimeMultipart) result).getBodyPart(0);
            if (firstBodyPart != null && firstBodyPart.getContentType().startsWith("text/"))
            {
                Object content = firstBodyPart.getContent();
                if (content instanceof String)
                {
                    return (String) content;
                }
            }
        }
        // No text content found either in message or in first body part of
        // MultiPart content
        return "";		
	}
	
	private class MailMessage
	{
		private String subject;
		private String content;
		private InternetAddress from;
		private InternetAddress to;
		private String messageId;
		private String inReplyTo;
		private List<String> references = new ArrayList<String>();
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			
			sb.append(getFrom() + " -> " + getTo() + ": " + messageId + " -> Reply To " + getInReplyTo());
			
			return sb.toString();
		}
		
		public String getSubject() {
			return subject;
		}
		public void setSubject(String subject) {
			this.subject = subject;
		}
		public String getContent() {
			return content;
		}
		public void setContent(String content) {
			this.content = content;
		}
		public InternetAddress getFrom() {
			return from;
		}
		public void setFrom(InternetAddress from) {
			this.from = from;
		}
		public InternetAddress getTo() {
			return to;
		}
		public void setTo(InternetAddress to) {
			this.to = to;
		}
		public String getMessageId() {
			return messageId;
		}
		public void setMessageId(String messageId) {
			this.messageId = messageId;
		}
		public String getInReplyTo() {
			return inReplyTo;
		}
		public void setInReplyTo(String inReplyTo) {
			this.inReplyTo = inReplyTo;
		}
		public List<String> getReferences() {
			return references;
		}
		public void setReferences(List<String> references) {
			this.references = references;
		}
		public void setReferences(String references) {
			if(references != null)
			{
				StringTokenizer st = new StringTokenizer(references);
				while(st.hasMoreTokens())
				{
					getReferences().add(st.nextToken());
				}
			}
		}
		public void setReferences(String references[]) {
			for(int i=0; references != null && i < references.length; i++)
			{
				getReferences().add(references[i]);
			}
		}	
	}
}
