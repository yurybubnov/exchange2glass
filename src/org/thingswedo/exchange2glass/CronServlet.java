package org.thingswedo.exchange2glass;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import microsoft.exchange.webservices.data.BasePropertySet;
import microsoft.exchange.webservices.data.EmailMessage;
import microsoft.exchange.webservices.data.EventType;
import microsoft.exchange.webservices.data.ExchangeCredentials;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.ExchangeVersion;
import microsoft.exchange.webservices.data.FolderId;
import microsoft.exchange.webservices.data.GetEventsResults;
import microsoft.exchange.webservices.data.ItemEvent;
import microsoft.exchange.webservices.data.ItemSchema;
import microsoft.exchange.webservices.data.PropertySet;
import microsoft.exchange.webservices.data.PullSubscription;
import microsoft.exchange.webservices.data.ServiceError;
import microsoft.exchange.webservices.data.ServiceResponseException;
import microsoft.exchange.webservices.data.WebCredentials;
import microsoft.exchange.webservices.data.WellKnownFolderName;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.mirror.model.TimelineItem;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class CronServlet extends HttpServlet {
	private static final Logger logger = Logger.getLogger(CronServlet.class.getName());

	private static final String itemPattern = "<article><section><div class=\"text-small\">%s</div><div class=\"text-normal\"><b>%s</b></div><div class=\"text-small\">%s</div></section></article>";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			String interval = req.getRequestURI().substring(req.getRequestURI().lastIndexOf('/') + 1);
			int inervalValue = Integer.valueOf(interval);
			logger.severe("Starting Cron for interval '" + interval + "'");
			DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
			Filter f = new FilterPredicate("interval", FilterOperator.EQUAL, interval);
			Query q = new Query(SaveSettingsServlet.SINGLE_SETTING_ENTOTY_NAME).setFilter(f);
			Iterable<Entity> list = datastoreService.prepare(q).asIterable();
			if (list == null) {
				logger.severe("List is empty");
				return;
			}
			ExchangeService service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
			List<FolderId> folder = new ArrayList<FolderId>();
			folder.add(FolderId.getFolderIdFromWellKnownFolderName(WellKnownFolderName.Inbox));

			for (Entity e : list) {
				String userID = e.getProperty("user").toString();
				String glassUserID = e.getProperty("glassUserID").toString();
				
				String lastWM = (e.getProperty("watermark") == null) ? null : e.getProperty("watermark").toString();
				String userName = e.getProperty("username").toString();
				String password = Utils.decodePassword(e.getProperty("password").toString());
				URI exchange = new URI(e.getProperty("exchange").toString());

				logger.severe("Current user " + userID);

				try {

					ExchangeCredentials credentials = new WebCredentials(userName, password);
					service.setCredentials(credentials);

					service.setUrl(exchange);

					PullSubscription subscription = service.subscribeToPullNotifications(folder, inervalValue * 2,
							lastWM, EventType.NewMail);
					GetEventsResults events = subscription.getEvents();
					// Loop through all item-related events.
					for (ItemEvent itemEvent : events.getItemEvents()) {
						if (itemEvent.getEventType() == EventType.NewMail) {

							PropertySet ps = new PropertySet(BasePropertySet.FirstClassProperties,
									ItemSchema.UniqueBody);
							EmailMessage message = EmailMessage.bind(service, itemEvent.getItemId(), ps);
							if (message.getIsRead()) {
								logger.severe("Message was read before: " + message.getSubject());
								continue;
							}

							String email = message.getFrom().getName();
							if (email == null || email.isEmpty()) {
								email = message.getFrom().getAddress();
							}

							String html = message.getUniqueBody().toString();
							html = html.replace("<html>", "");
							html = html.replace("</html>", "");
							html = html.replace("<body>", "");
							html = html.replace("</body>", "");

							String content = String.format(itemPattern, email, message.getSubject(), html);

							TimelineItem item = new TimelineItem();
							item.setHtml(content);

							Credential credential = AuthUtil.getCredential(glassUserID);
							logger.severe("Inserting for User:" + userID + " Item: " + item.toPrettyString());
							MirrorClient.insertTimelineItem(credential, item);
						}
					}

					e.setProperty("watermark", subscription.getWaterMark());
					datastoreService.put(e);

				} catch (ServiceResponseException ex) {
					logger.severe("Cannot process mail for user " + userID + " Exception: " + ex.getMessage());
					if (ex.getErrorCode() == ServiceError.ErrorExpiredSubscription) {
						PullSubscription subscription = service.subscribeToPullNotifications(folder, inervalValue * 2,
								lastWM, EventType.NewMail);
						e.setProperty("watermark", subscription.getWaterMark());
						datastoreService.put(e);
					} else {
						throw new RuntimeException(ex);
					}
				}

			}
		} catch (Exception e) {
			logger.severe("Cannot execute cron job " + e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
