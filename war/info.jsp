<%@page import="org.thingswedo.exchange2glass.SaveSettingsServlet"%>
<%@page import="com.google.appengine.api.datastore.Query.FilterOperator"%>
<%@page import="com.google.appengine.api.datastore.Query.Filter"%>
<%@page
	import="com.google.appengine.api.datastore.Query.FilterPredicate"%>
<%@page import="com.google.appengine.api.datastore.Entity"%>
<%@page import="com.google.appengine.api.datastore.Query"%>
<%@page
	import="com.google.appengine.api.datastore.DatastoreServiceFactory"%>
<%@page import="com.google.appengine.api.datastore.DatastoreService"%>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ page import="java.util.List"%>
<%@ page import="com.google.appengine.api.users.User"%>
<%@ page import="com.google.appengine.api.users.UserService"%>
<%@ page import="com.google.appengine.api.users.UserServiceFactory"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<html>
<head>
<title>Exchange2Glass</title>
</head>
<body>

	<%
		UserService userService = UserServiceFactory.getUserService();
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
		User user = userService.getCurrentUser();
		if (user == null) {
			response.sendRedirect(userService.createLoginURL(request.getRequestURI()));
			return;
		}
		pageContext.setAttribute("user", user);
		Filter f = new FilterPredicate("user", FilterOperator.EQUAL, user.getUserId());
		Query q = new Query(SaveSettingsServlet.SINGLE_SETTING_ENTOTY_NAME).setFilter(f);
		Entity e = datastoreService.prepare(q).asSingleEntity();
	%>
	<p>
		Hello, ${fn:escapeXml(user.nickname)}! (You can <a href="/signout">delete
			subscription</a> or <a href="<%=userService.createLogoutURL("/")%>">sign
			out</a>.)
	</p>
	<br>

	<form action="saveSettings" method="post">
		<p>
			Username: <input type="text" name="username"
				value="<%=(e == null) ? "" : e.getProperty("username")%>" />
		</p>
		<p>
			Password: <input type="password" name="password" />
		</p>
		<p>
			Service URL: <input type="text" name="exchange"
				value="<%=(e == null) ? "" : e.getProperty("exchange")%>" />
		</p>
		<p>
			Pull interval: <select name="interval">
				<option value="5"
					<%=(e != null && "5".equalsIgnoreCase(e.getProperty("interval").toString())) ? "selected=\"selected\""
					: ""%>>5
					min</option>
				<option value="10"
					<%=(e != null && "10".equalsIgnoreCase(e.getProperty("interval").toString())) ? "selected=\"selected\""
					: ""%>>10
					min</option>
				<option value="15"
					<%=(e != null && "15".equalsIgnoreCase(e.getProperty("interval").toString())) ? "selected=\"selected\""
					: ""%>>15
					min</option>
				<option value="30"
					<%=(e != null && "30".equalsIgnoreCase(e.getProperty("interval").toString())) ? "selected=\"selected\""
					: ""%>>30
					min</option>
				<option value="60"
					<%=(e != null && "60".equalsIgnoreCase(e.getProperty("interval").toString())) ? "selected=\"selected\""
					: ""%>>1
					hour</option>
			</select>
		</p>
		<p>
			<input type="submit" value="Submit" />
		</p>

	</form>
</body>
</html>
