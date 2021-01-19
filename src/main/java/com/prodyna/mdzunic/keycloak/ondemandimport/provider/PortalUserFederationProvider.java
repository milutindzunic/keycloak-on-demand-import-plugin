package com.prodyna.mdzunic.keycloak.ondemandimport.provider;

import com.prodyna.mdzunic.keycloak.ondemandimport.provider.helper.UserDataRemoteService;
import com.prodyna.mdzunic.keycloak.ondemandimport.provider.helper.ValidateUserCredentialsRemoteService;
import com.prodyna.mdzunic.keycloak.ondemandimport.provider.helper.dto.UserCredentials;
import com.prodyna.mdzunic.keycloak.ondemandimport.provider.helper.dto.UserResponse;
import org.jboss.logging.Logger;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PortalUserFederationProvider implements UserStorageProvider, UserLookupProvider, CredentialInputValidator {

    private static final Logger LOG = Logger.getLogger(PortalUserFederationProvider.class);
    private static final Set<String> CREDENTIAL_TYPES = Collections.singleton(PasswordCredentialModel.TYPE);

    private final ValidateUserCredentialsRemoteService userCredentialsService;
    private final UserDataRemoteService userService;

    protected KeycloakSession session;
    protected ComponentModel model;
    // map of loaded users in this transaction
    protected Map<String, UserModel> loadedUsers = new HashMap<>();

    public PortalUserFederationProvider(KeycloakSession session, ComponentModel model) {
        LOG.info("Instantiating on-demand Portal import provider...");
        this.session = session;
        this.model = model;

        MultivaluedHashMap<String, String> config = model.getConfig();
        userCredentialsService = new ValidateUserCredentialsRemoteService(
                config.getFirst(PortalUserFederationProviderFactory.PORTAL_BASE_URL),
                config.getFirst(PortalUserFederationProviderFactory.AUTH_PRINCIPAL),
                config.getFirst(PortalUserFederationProviderFactory.AUTH_SECRET)
        );
        userService = new UserDataRemoteService(
                config.getFirst(PortalUserFederationProviderFactory.PORTAL_BASE_URL),
                config.getFirst(PortalUserFederationProviderFactory.PORTAL_COMPANY_ID),
                config.getFirst(PortalUserFederationProviderFactory.AUTH_PRINCIPAL),
                config.getFirst(PortalUserFederationProviderFactory.AUTH_SECRET)
        );
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return CREDENTIAL_TYPES.contains(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realmModel, UserModel userModel, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realmModel, UserModel userModel, CredentialInput credentialInput) {
        if (!(credentialInput instanceof UserCredentialModel)) {
            return false;
        }

        if (!supportsCredentialType(credentialInput.getType())) {
            return false;
        }

        LOG.infof("Validating credentials with Portal for User: %s", userModel.getEmail());
        boolean valid = validPassword(userModel.getEmail(), credentialInput.getChallengeResponse());

        if (valid) {
            LOG.infof("Portal credentials validation successful! Updating user's credentials in local storage and removing the federation link. User: %s", userModel.getEmail());
            session.userCredentialManager().updateCredential(realmModel, userModel, credentialInput);
            userModel.setFederationLink(null);
        } else {
            LOG.infof("Portal credentials validation unsuccessful! User: %s", userModel.getEmail());
        }

        return valid;
    }

    protected boolean validPassword(String email, String password) {
        UserCredentials credentials = new UserCredentials();
        credentials.setEmail(email);
        credentials.setPassword(password);

        return userCredentialsService.validateCredentials(credentials);
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public UserModel getUserById(String id, RealmModel realmModel) {
        LOG.infof("Getting User by id: %s", id);

        StorageId storageId = new StorageId(id);
        String username = storageId.getExternalId();
        return getUserByUsername(username, realmModel);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realmModel) {
        LOG.infof("Getting User by username: %s", username);

        UserModel adapter = loadedUsers.get(username);
        if (adapter == null) {
            UserResponse remoteUser = userService.fetchUserByUsername(username);
            if (remoteUser != null) {
                adapter = createAdapter(realmModel, remoteUser);
                loadedUsers.put(username, adapter);
            }
        }

        return adapter;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realmModel) {
        LOG.infof("Getting User by email: %s", email);

        UserModel adapter = loadedUsers.get(email);
        if (adapter == null) {
            UserResponse remoteUser = userService.fetchUserByEmail(email);
            if (remoteUser != null) {
                adapter = createAdapter(realmModel, remoteUser);
                loadedUsers.put(email, adapter);
            }
        }

        return adapter;
    }

    protected UserModel createAdapter(RealmModel realm, UserResponse remoteUser) {
        UserModel local = session.userLocalStorage().getUserByUsername(remoteUser.getScreenName(), realm);
        if (local == null) {
            LOG.infof("User not found in local storage, creating user... email: %s screenName: %s", remoteUser.getEmailAddress(), remoteUser.getScreenName());

            local = session.userLocalStorage().addUser(realm, remoteUser.getScreenName());
            local.setEmail(remoteUser.getEmailAddress());
            local.setFirstName(remoteUser.getFirstName());
            local.setLastName(remoteUser.getLastName());
            local.setEnabled(remoteUser.getStatus() == 0); // 0 - Status.APPROVED
            local.setFederationLink(model.getId());
        }

        return new UserModelDelegate(local);

    }
}
