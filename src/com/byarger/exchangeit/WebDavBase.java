package com.byarger.exchangeit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WebDavBase {

	private String url;
	private String username;
	private String password;

	public WebDavBase(String url, String username, String password) {
		this.url = url;
		this.username = username;
		this.password = password;
	}

	public String getUrl() {
		return url;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	protected static DefaultHttpClient createHttpClient() {
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		// http scheme
		schemeRegistry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		// https scheme
		schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(),
				443));

		HttpParams params = new BasicHttpParams();
		params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
		params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE,
				new ConnPerRouteBean(30));
		params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

		ClientConnectionManager cm = new ThreadSafeClientConnManager(params,
				schemeRegistry);

		return new DefaultHttpClient(cm, params);
	}

	protected static boolean authenticate(DefaultHttpClient client, String url,
			String username, String password) throws ClientProtocolException,
			IOException {
		HttpGet get = new HttpGet(url);

		// auth problem, lets try forms based auth
		formsBasedAuth2003(client, url, username, password);

		// redo the request, see if the cookies work
		HttpResponse response = client.execute(get);

		int code = response.getStatusLine().getStatusCode();

		if (code == 401) {
			formsBasedAuth2007(client, url, username, password);

			// redo the request, see if the cookies work
			response = client.execute(get);
			code = response.getStatusLine().getStatusCode();
		}
		return code < 300;
	}

	private static int formsBasedAuth2003(HttpClient client, String url,
			String username, String password) throws ClientProtocolException,
			IOException {
		// get the base of the URL

		HttpPost fbaAuth = new HttpPost(
				getBaseUrl(url)
						+ "/exchweb/bin/auth/owaauth.dll?ForcedBasic=false&amp;Basic=false&amp;Private=true&amp;Language=No_Value");
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("destination", url));
		nvps.add(new BasicNameValuePair("username", username));
		nvps.add(new BasicNameValuePair("password", password));

		fbaAuth.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

		HttpResponse response = client.execute(fbaAuth);

		return response.getStatusLine().getStatusCode();
	}

	private static int formsBasedAuth2007(HttpClient client, String url,
			String username, String password) throws ClientProtocolException,
			IOException {
		// get the base of the URL

		HttpPost fbaAuth = new HttpPost(getBaseUrl(url)
				+ "/owa/auth/owaauth.dll");
		fbaAuth.getParams().setParameter("destination", url);
		fbaAuth.getParams().setParameter("username", username);
		fbaAuth.getParams().setParameter("password", password);
		HttpResponse response = client.execute(fbaAuth);

		return response.getStatusLine().getStatusCode();
	}

	protected static String getBaseUrl(String fullUrl) {
		int idx = fullUrl.indexOf("//");
		if (idx == -1) {
			return "";
		}
		int nextIdx = fullUrl.indexOf("/", idx + 2);
		if (idx == -1) {
			return "";
		}
		return fullUrl.substring(0, nextIdx);
	}

	protected static String getPathURI(String fullUrl) {
		int idx = fullUrl.indexOf("//");
		if (idx == -1) {
			return "";
		}
		int nextIdx = fullUrl.indexOf("/", idx + 2);
		if (idx == -1) {
			return "";
		}
		return fullUrl.substring(nextIdx);
	}

	protected static String getSingleValue(NodeList nl) {
		if (nl != null && nl.getLength() > 0) {
			Node item = nl.item(0);
			if (item.getNodeType() == Node.ELEMENT_NODE) {
				return getSingleValue(item.getChildNodes());
			} else if (item.getNodeType() == Node.TEXT_NODE) {
				return nl.item(0).getNodeValue();
			}
		}
		return "";
	}

}
