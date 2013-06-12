package org.thingswedo.exchange2glass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.http.*;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.mirror.model.MenuItem;
import com.google.api.services.mirror.model.MenuValue;
import com.google.api.services.mirror.model.NotificationConfig;
import com.google.api.services.mirror.model.TimelineItem;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@SuppressWarnings("serial")
public class Exchange2glassServlet extends HttpServlet {
	private static final Logger LOG = Logger
			.getLogger(Exchange2glassServlet.class.getSimpleName());

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		Credential credential = AuthUtil.getCredential(req);

		MirrorClient.insertSubscription(credential,
				WebUtil.buildUrl(req, "/notify"), AuthUtil.getUserId(req),
				"timeline");

		TimelineItem item = new TimelineItem();
		item.setTitle("This is title");
		item.setText("This is text");

		MenuItem mi = new MenuItem().setAction("REPLY").setId("retake");
		MenuValue mv = new MenuValue();
		mv.setDisplayName("Retake");
		mv.setState("DEFAULT");
		mi.setValues(Arrays.asList(mv));
		item.setMenuItems(Arrays.asList(mi));
		//item.setNotification(new NotificationConfig().setLevel("audio_only"));
		item.setSourceItemId("USER_ID#ITEM_ID");
		item = MirrorClient.insertTimelineItem(credential, item);
		LOG.severe("Sent item: " + item.toPrettyString());
	}
}
