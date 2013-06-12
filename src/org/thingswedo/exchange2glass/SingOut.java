package org.thingswedo.exchange2glass;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.mirror.model.Subscription;
import com.google.api.services.mirror.model.SubscriptionsListResponse;

public class SingOut extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		Credential credential = AuthUtil.getCredential(req);
		SubscriptionsListResponse subscriptions = MirrorClient
				.listSubscriptions(credential);
		if (subscriptions != null && subscriptions.getItems() != null) {
			for (Subscription subscription : subscriptions.getItems()) {
				MirrorClient.deleteContact(credential, subscription.getId());
			}
		}
		
		AuthUtil.clearUserId(req);
	}
}
