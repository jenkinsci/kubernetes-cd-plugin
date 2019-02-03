package com.microsoft.jenkins.kubernetes.helm;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.UsernameCredentials;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.ListBoxModel;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collections;

public class HelmRepositoryEndPoint extends AbstractDescribableImpl<HelmRepositoryEndPoint>
        implements Serializable {
    private final String name;
    private final String url;
    private final String credentialsId;

    @DataBoundConstructor
    public HelmRepositoryEndPoint(String name, String url, String credentialsId) {
        this.name = StringUtils.trimToNull(name);
        this.url = StringUtils.trimToNull(url);
        this.credentialsId = StringUtils.trimToNull(credentialsId);
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<HelmRepositoryEndPoint> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Helm Repository";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item) {
            if (item == null && !Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER)
                    || item != null && !item.hasPermission(Item.EXTENDED_READ)) {
                return new StandardListBoxModel();
            }
            // TODO may also need to specify a specific authentication and domain requirements
            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .withMatching(AuthenticationTokens.matcher(UsernameCredentials.class),
                            CredentialsProvider.lookupCredentials(
                                    StandardCredentials.class,
                                    item,
                                    null,
                                    Collections.emptyList()
                            )
                    );
        }

    }
}
