package fi.bitrite.android.ws.host;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import fi.bitrite.android.ws.activity.model.HostInformation;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.auth.NoAccountException;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationFailedException;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.http.HttpException;

public interface Search {

    public List<HostBriefInfo> doSearch() throws JSONException, HttpException, IOException;

}
