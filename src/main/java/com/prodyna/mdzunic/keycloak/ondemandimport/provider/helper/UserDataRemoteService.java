package com.prodyna.mdzunic.keycloak.ondemandimport.provider.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodyna.mdzunic.keycloak.ondemandimport.provider.helper.dto.UserResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class UserDataRemoteService {

    protected static final Logger LOG = Logger.getLogger(UserDataRemoteService.class);

    private final String serviceUrl;
    private final String portalCompanyId;
    private final String authUser;
    private final String authPassword;

    public UserDataRemoteService(String serviceUrl, String portalCompanyId, String authUser, String authPassword) {
        this.serviceUrl = serviceUrl;
        this.portalCompanyId = portalCompanyId;
        this.authUser = authUser;
        this.authPassword = authPassword;
    }

    public UserResponse fetchUserByEmail(String email) {

        try {
            URI uri = createFetchByEmailUri(email);
            return fetchUserData(uri);
        } catch (Exception e) {
            LOG.error("Error fetching User data!", e);
            return null;
        }
    }

    public UserResponse fetchUserByUsername(String username) {

        try {
            URI uri = createFetchByScreenNameUri(username);
            return fetchUserData(uri);
        } catch (Exception e) {
            LOG.error("Error fetching User data!", e);
            return null;
        }
    }

    private UserResponse fetchUserData(URI uri) throws IOException, AuthenticationException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(uri);

            LOG.info("Fetching User data from: " + httpGet.getURI().toString());

            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(authUser, authPassword);
            httpGet.addHeader(new BasicScheme(StandardCharsets.UTF_8).authenticate(credentials, httpGet, null));
            httpGet.addHeader("Accept", MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(httpGet)) {

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

                    UserResponse user = new ObjectMapper().readValue(response.getEntity().getContent(), UserResponse.class);
                    LOG.info("Read User data for user: " + user.getEmailAddress());
                    LOG.debug("Read User data: " + user);

                    return user;
                } else {
                    LOG.error("Error fetching User data! Unsuccessful status: " + response.getStatusLine().getStatusCode());
                    LOG.error("Error fetching User data! Received response: " + EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
                    return null;
                }
            }
        }

    }

    private URI createFetchByEmailUri(String email) {
        UriBuilder uriBuilder = UriBuilder.fromUri(serviceUrl + "/api/jsonws/user/get-user-by-email-address");
        uriBuilder.queryParam("companyId", portalCompanyId);
        uriBuilder.queryParam("emailAddress", email);
        return uriBuilder.build();
    }

    private URI createFetchByScreenNameUri(String screenName) {
        UriBuilder uriBuilder = UriBuilder.fromUri(serviceUrl + "/api/jsonws/user/get-user-by-screen-name");
        uriBuilder.queryParam("companyId", portalCompanyId);
        uriBuilder.queryParam("screenName", screenName);
        return uriBuilder.build();
    }

}
