package com.prodyna.mdzunic.keycloak.ondemandimport.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;

public class PortalUserFederationProviderFactory implements UserStorageProviderFactory<PortalUserFederationProvider> {

    protected static final List<ProviderConfigProperty> configMetadata;

    public static final String PORTAL_BASE_URL = "portal-base-url";
    public static final String PORTAL_COMPANY_ID = "portal-company-id";
    public static final String AUTH_PRINCIPAL = "auth-principal";
    public static final String AUTH_SECRET = "auth-secret";

    static {
        configMetadata = ProviderConfigurationBuilder.create()
                .property().name(PORTAL_BASE_URL)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Portal base url").add()
                .property().name(PORTAL_COMPANY_ID)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Portal company id").add()
                .property().name(AUTH_PRINCIPAL)
                .secret(true)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Auth Principal for the credential validation/user data calls").add()
                .property().name(AUTH_SECRET)
                .secret(true)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Auth Secret for the credential validation/user data calls").add()
                .build();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config)
            throws ComponentValidationException {

        checkPropertiesAreSet(config,
                PORTAL_BASE_URL,
                PORTAL_COMPANY_ID,
                AUTH_PRINCIPAL,
                AUTH_SECRET
        );
    }

    private void checkPropertiesAreSet(ComponentModel config, String... props) {
        for (String prop : props) {
            checkPropertyIsSet(config, prop);
        }
    }

    private void checkPropertyIsSet(ComponentModel config, String prop) {
        String propValue = config.getConfig().getFirst(prop);
        if (propValue == null || propValue.isEmpty()) {
            throw new ComponentValidationException("Credentials validation url is missing!");
        }
    }

    @Override
    public PortalUserFederationProvider create(KeycloakSession session, ComponentModel model) {
        return new PortalUserFederationProvider(session, model);
    }

    @Override
    public String getId() {
        return "portal-user-federation-provider";
    }

}
