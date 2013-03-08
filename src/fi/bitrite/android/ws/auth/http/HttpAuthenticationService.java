package fi.bitrite.android.ws.auth.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.AuthenticationService;
import fi.bitrite.android.ws.host.impl.HttpHostIdScraper;
import fi.bitrite.android.ws.util.http.HttpUtils;

/**
 * Responsible for authenticating the user against the WarmShowers web service.
 */
@Singleton
public class HttpAuthenticationService {

	private static final String WARMSHOWERS_USER_AUTHENTICATION_URL = "http://www.warmshowers.org/services/rest/user/login";

	private static final String WARMSHOWERS_USER_AUTHENTICATION_TEST_URL = "http://www.warmshowers.org/search/wsuser";

	@Inject
	HttpSessionContainer sessionContainer;

	String username;
	String authtoken;

	/**
	 * Load a page in order to see if we are authenticated
	 */
	public boolean isAuthenticated() {
		HttpClient client = HttpUtils.getDefaultClient();
		int responseCode;
		try {
			String url = HttpUtils.encodeUrl(WARMSHOWERS_USER_AUTHENTICATION_TEST_URL);
			HttpGet get = new HttpGet(url);
			HttpContext context = sessionContainer.getSessionContext();

			HttpResponse response = client.execute(get, context);
			HttpEntity entity = response.getEntity();
			responseCode = response.getStatusLine().getStatusCode();
			EntityUtils.toString(entity, "UTF-8");
		}
		
		catch (Exception e) {
			throw new HttpAuthenticationFailedException(e);
		}

		finally {
			client.getConnectionManager().shutdown();
		}
		
		return (responseCode == HttpStatus.SC_OK);
	}

	public void authenticate() {
		try {
			getCredentialsFromAccount();
			authenticate(username, authtoken);
		}
		
		catch (Exception e) {
			throw new HttpAuthenticationFailedException(e);
		}
	}

	private void getCredentialsFromAccount() throws OperationCanceledException, AuthenticatorException, IOException {
		AccountManager accountManager = AccountManager.get(WSAndroidApplication.getAppContext());
		Account account = AuthenticationHelper.getWarmshowersAccount();

		authtoken = accountManager.blockingGetAuthToken(account, AuthenticationService.ACCOUNT_TYPE, true);
		username = account.name;
	}
	
	public int authenticate(String username, String password) {
		HttpClient client = HttpUtils.getDefaultClient();
		HttpContext httpContext = sessionContainer.getSessionContext();
		CookieStore cookieStore = (CookieStore) httpContext.getAttribute(ClientContext.COOKIE_STORE);
		cookieStore.clear();
		int userId = 0;
		
		try {
			List<NameValuePair> credentials = generateCredentialsForPost(username, password);
			HttpPost post = new HttpPost(WARMSHOWERS_USER_AUTHENTICATION_URL);
			post.setEntity(new UrlEncodedFormEntity(credentials));
			HttpResponse response = client.execute(post, httpContext);

			HttpEntity entity = response.getEntity();
			String rawJson = EntityUtils.toString(entity, "UTF-8");

            JsonParser parser = new JsonParser();
            JsonObject o = (JsonObject) parser.parse(rawJson);
            String s = o.get("user").getAsJsonObject().get("uid").getAsString();
            userId = Integer.valueOf(s);
		}
		
		catch (ClientProtocolException e) {
			if (e.getCause() instanceof CircularRedirectException) {
				// If we getHost this authentication has still been successful, so ignore it
			} else {
				throw new HttpAuthenticationFailedException(e);
			}
		}		
		
		catch (Exception e) {
			throw new HttpAuthenticationFailedException(e);
		}

		finally {
			client.getConnectionManager().shutdown();
		}

		if (!isAuthenticated()) {
			throw new HttpAuthenticationFailedException("Invalid credentials");
		}
		
		return userId;
	}

	private List<NameValuePair> generateCredentialsForPost(String username, String password) {
		List<NameValuePair> args = new ArrayList<NameValuePair>();
		args.add(new BasicNameValuePair("username", username));
		args.add(new BasicNameValuePair("password", password));
		return args;
	}
}