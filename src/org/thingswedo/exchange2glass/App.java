package org.thingswedo.exchange2glass;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import microsoft.exchange.webservices.data.EmailMessage;
import microsoft.exchange.webservices.data.EventType;
import microsoft.exchange.webservices.data.ExchangeCredentials;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.ExchangeVersion;
import microsoft.exchange.webservices.data.Folder;
import microsoft.exchange.webservices.data.FolderEvent;
import microsoft.exchange.webservices.data.FolderId;
import microsoft.exchange.webservices.data.GetEventsResults;
import microsoft.exchange.webservices.data.ItemEvent;
import microsoft.exchange.webservices.data.PullSubscription;
import microsoft.exchange.webservices.data.WebCredentials;
import microsoft.exchange.webservices.data.WellKnownFolderName;

/**
 * Hello world!
 * 
 */
public class App {
	public static void main(String[] args) throws Exception {
		String wm = null;
		while (true) {
			ExchangeService service = new ExchangeService(
					ExchangeVersion.Exchange2010_SP2);
			ExchangeCredentials credentials = new WebCredentials("ybubnov",
					"BakaeBay0613&");
			service.setCredentials(credentials);

			service.setUrl(new URI(
					"https://molecule.corp.ebay.com/ews/Exchange.asmx"));

			// Folder sent = Folder.bind(service,
			// WellKnownFolderName.SentItems);
			/*
			 * EmailMessage msg = new EmailMessage(service);
			 * msg.setSubject("Hello from exchange5!"); msg.setBody(MessageBody
			 * .getMessageBodyFromText("Sent using the EWS Managed API."));
			 * msg.getToRecipients().add("ybubnov@gmail.com");
			 * msg.sendAndSaveCopy(WellKnownFolderName.SentItems); //
			 * msg.move(WellKnownFolderName.Outbox)
			 */
			// Subscribe to pull notifications in the Inbox folder, and get
			// notified
			// when a new mail is received, when an item or folder is created,
			// or
			// when an item or folder is deleted.

			List folder = new ArrayList();
			folder.add(FolderId
					.getFolderIdFromWellKnownFolderName(WellKnownFolderName.Inbox));

			PullSubscription subscription = service
					.subscribeToPullNotifications(folder, 5
					/*
					 * timeOut: the subscription will end if the server is not
					 * polled within 5 minutes.
					 */, wm /*
							 * watermark: null to start a new subscription .
							 */, EventType.NewMail, EventType.Created);
			System.out.println("Waiting----------");
			// Thread.sleep(1000);
			// Wait a couple minutes, then poll the server for new events.
			GetEventsResults events = subscription.getEvents();
			// Loop through all item-related events.
			for (ItemEvent itemEvent : events.getItemEvents()) {
				if (itemEvent.getEventType() == EventType.NewMail) {
					EmailMessage message = EmailMessage.bind(service,
							itemEvent.getItemId());
					// if (message.isNew()) {
					System.out.println(message.isNew() + " New: "
							+ message.getSubject());
					// }
				}
				if (itemEvent.getEventType() == EventType.Created) {
					EmailMessage message = EmailMessage.bind(service,
							itemEvent.getItemId());
					// if (message.isNew()) {
					System.out.println(message.isNew() + " CR: "
							+ message.getSubject());
					// }
				}

			}

			// Loop through all folder-related events.
			for (FolderEvent folderEvent : events.getFolderEvents()) {
				if (folderEvent.getEventType() == EventType.Created) {
					Folder f1 = Folder.bind(service, folderEvent.getFolderId());

				} else if (folderEvent.getEventType() == EventType.Deleted) {
					System.out.println("folder  deleted"
							+ folderEvent.getFolderId().getUniqueId());
				}
			}
			System.out.println("OLD WM:" + subscription.getWaterMark());
			wm = subscription.getWaterMark();
			// subscription.unsubscribe();
			System.out.println("Sleep");
			Thread.sleep(15000);
		}
	}
}