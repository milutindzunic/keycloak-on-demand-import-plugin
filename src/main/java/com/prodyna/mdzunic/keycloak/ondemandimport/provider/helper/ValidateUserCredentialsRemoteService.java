package com.prodyna.mdzunic.keycloak.ondemandimport.provider.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodyna.mdzunic.keycloak.ondemandimport.provider.helper.dto.UserCredentials;
import com.prodyna.mdzunic.keycloak.ondemandimport.provider.helper.dto.UserCredentialsResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ValidateUserCredentialsRemoteService {

    protected static final Logger LOG = Logger.getLogger(ValidateUserCredentialsRemoteService.class);

    private final String validationUrl;
    private final String authUser;
    private final String authPassword;

    public ValidateUserCredentialsRemoteService(String serviceUrl, String authUser, String authPassword) {
        this.validationUrl = serviceUrl;
        this.authUser = authUser;
        this.authPassword = authPassword;
    }

    public boolean validateCredentials(UserCredentials userCredentials) {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(validationUrl + "/o/validateUserCredentials");

            LOG.info("Validating User credentials via: " + httpPost.getURI().toString());

            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(authUser, authPassword);
            httpPost.addHeader(new BasicScheme().authenticate(credentials, httpPost, null));
            httpPost.addHeader("Content-Type", MediaType.APPLICATION_JSON);

            httpPost.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(userCredentials)));

            try (CloseableHttpResponse response = client.execute(httpPost)) {

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

                    UserCredentialsResponse credentialsResponse = new ObjectMapper().readValue(response.getEntity().getContent(), UserCredentialsResponse.class);
                    LOG.debug("Read response: " + response);

                    return credentialsResponse.isValid();
                } else {
                    LOG.error("Error validating User credentials! Unsuccessful status: " + response.getStatusLine().getStatusCode());
                    LOG.error("Error validating User credentials! Received response: " + EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
                    throw new RuntimeException("Error checking User credentials!");
                }
            }
        } catch (IOException | AuthenticationException e) {
            LOG.error("Error validating User credentials!" + e);
            return false;
        }

    }
}
