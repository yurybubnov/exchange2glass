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

import microsoft.exchange.webservices.data.EmailMessage;
import microsoft.exchange.webservices.data.EventType;
import microsoft.exchange.webservices.data.ExchangeCredentials;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.ExchangeVersion;
import microsoft.exchange.webservices.data.FolderId;
import microsoft.exchange.webservices.data.GetEventsResults;
import microsoft.exchange.webservices.data.ItemEvent;
import microsoft.exchange.webservices.data.PullSubscription;
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
	private static final Logger logger = Logger.getLogger(CronServlet.class
			.getName());

	private static final String itemPattern = "<article><section><div class=\"text-small\">%s</div><div class=\"text-normal\"><b>%s</b></div><div class=\"text-small\">%s</div></section></article>";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			String interval = req.getRequestURI().substring(
					req.getRequestURI().lastIndexOf('/') + 1);
			logger.info("Starting Cron for interval '" + interval + "'");
			DatastoreService datastoreService = DatastoreServiceFactory
					.getDatastoreService();
			Filter f = new FilterPredicate("interval", FilterOperator.EQUAL,
					interval);
			Query q = new Query(SaveSettingsServlet.SINGLE_SETTING_ENTOTY_NAME)
					.setFilter(f);
			Iterable<Entity> list = datastoreService.prepare(q).asIterable();
			if (list == null) {
				logger.info("List is empty");
				return;
			}

			for (Entity e : list) {
				try {
					logger.info("Current user "
							+ e.getProperty("username").toString());
					logger.info("Current password "
							+ Utils.decodePassword(e.getProperty("password")
									.toString()));
					logger.info("Current URL "
							+ e.getProperty("exchange").toString());

					String lastWM = (e.getProperty("watemark") == null) ? null
							: e.getProperty("watemark").toString();

					ExchangeService service = new ExchangeService(
							ExchangeVersion.Exchange2010_SP2);
					ExchangeCredentials credentials = new WebCredentials(e
							.getProperty("username").toString(),
							Utils.decodePassword(e.getProperty("password")
									.toString()));
					service.setCredentials(credentials);

					service.setUrl(new URI(e.getProperty("exchange").toString()));

					List<FolderId> folder = new ArrayList<FolderId>();
					folder.add(FolderId
							.getFolderIdFromWellKnownFolderName(WellKnownFolderName.Inbox));

					PullSubscription subscription = service
							.subscribeToPullNotifications(folder, 5, lastWM,
									EventType.NewMail);
					GetEventsResults events = subscription.getEvents();
					// Loop through all item-related events.
					for (ItemEvent itemEvent : events.getItemEvents()) {
						if (itemEvent.getEventType() == EventType.NewMail) {
							EmailMessage message = EmailMessage.bind(service,
									itemEvent.getItemId());

							String email = message.getFrom().getName();
							if (email == null || email.isEmpty()) {
								email = message.getFrom().getAddress();
							}
							String html = String.format(itemPattern, email,
									message.getSubject(), message
											.getUniqueBody().getText());

							TimelineItem item = new TimelineItem();
							item.setHtml(html);

							Credential credential = AuthUtil.getCredential(e
									.getProperty("user").toString());
							logger.info("Inserting for User:"
									+ e.getProperty("user").toString()
									+ " Item: " + item.toPrettyString());
							MirrorClient.insertTimelineItem(credential, item);
						}
					}

					e.setProperty("watermark", subscription.getWaterMark());
					datastoreService.put(e);

				} catch (Exception ex) {
					logger.severe("Cannot process mail for user "
							+ e.getProperty("user") + " Exception: "
							+ ex.getMessage());
					throw new RuntimeException(ex);
				}

			}
		} catch (Exception e) {
			logger.severe("Cannot execute cron job "+e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
