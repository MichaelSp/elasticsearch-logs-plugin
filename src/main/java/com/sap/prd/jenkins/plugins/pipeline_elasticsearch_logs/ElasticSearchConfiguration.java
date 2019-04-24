package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;

public class ElasticSearchConfiguration extends AbstractDescribableImpl<ElasticSearchConfiguration>
{
  @Nonnull
  private String host;

  private int port;

  @Nonnull
  private String key;

  private boolean ssl;

  private String certificateId;

  @CheckForNull
  private String credentialsId;

  @DataBoundConstructor
  public ElasticSearchConfiguration(String host, int port, String key)
  {
    this.host = host;
    this.port = port;
    this.key = key;
  }

  public boolean isSsl()
  {
    return ssl;
  }

  @DataBoundSetter
  public void setSsl(boolean ssl)
  {
    this.ssl = ssl;
  }

  public String getCertificateId()
  {
    return certificateId;
  }

  @DataBoundSetter
  public void setCertificateId(String certificateId)
  {
    this.certificateId = certificateId;
  }

  public String getKey()
  {
    return key;
  }

  @CheckForNull
  public String getCredentialsId()
  {
    return credentialsId;
  }

  @DataBoundSetter
  public void setCredentialsId(String credentialsId)
  {
    this.credentialsId = credentialsId;
  }

  public String getHost()
  {
    return host;
  }

  public int getPort()
  {
    return port;
  }

  @CheckForNull
  private StandardUsernamePasswordCredentials getCredentials()
  {
    if (credentialsId == null)
    {
      return null;
    }
    return getCredentials(credentialsId);
  }

  @CheckForNull
  private static StandardUsernamePasswordCredentials getCredentials(@Nonnull String id)
  {
    StandardUsernamePasswordCredentials credential = null;
    List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(
        StandardUsernamePasswordCredentials.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList());
    IdMatcher matcher = new IdMatcher(id);
    for (StandardUsernamePasswordCredentials c : credentials)
    {
      if (matcher.matches(c))
      {
        credential = c;
      }
    }
    return credential;
  }

  @CheckForNull
  private static StandardCertificateCredentials getCertificateCredentials(@Nonnull String id)
  {
    StandardCertificateCredentials credential = null;
    List<StandardCertificateCredentials> credentials = CredentialsProvider.lookupCredentials(
        StandardCertificateCredentials.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList());
    IdMatcher matcher = new IdMatcher(id);
    for (StandardCertificateCredentials c : credentials)
    {
      if (matcher.matches(c))
      {
        credential = c;
      }
    }
    return credential;
  }

  private KeyStore getCustomKeyStore()
  {
    KeyStore customKeyStore = null;

    if (!StringUtils.isBlank(certificateId))
    {
      StandardCertificateCredentials certificateCredentials = getCertificateCredentials(certificateId);
      if (certificateCredentials != null)
      {
        customKeyStore = certificateCredentials.getKeyStore();
      }
    }
    return customKeyStore;
  }

  private byte[] getKeyStoreBytes()
  {
    KeyStore keyStore = getCustomKeyStore();
    if (isSsl() && keyStore != null)
    {
      ByteArrayOutputStream b = new ByteArrayOutputStream(2048);
      try
      {
        keyStore.store(b, "".toCharArray());
        return b.toByteArray();
      }
      catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return null;
  }

  public ElasticSearchSerializableConfiguration getSerializableConfiguration()
  {
    String username = null;
    String password = null;
    StandardUsernamePasswordCredentials credentials = getCredentials();
    if (credentials != null)
    {
      username = credentials.getUsername();
      password = Secret.toString(credentials.getPassword());
    }

    String key = getKey();
    if (!key.startsWith("/"))
    {
      key = "/" + key;
    }

    return new ElasticSearchSerializableConfiguration(host, port, key, username, password, ssl, getKeyStoreBytes());
  }

  @Extension
  @Symbol("elasticsearch")
  public static class DescriptorImpl extends Descriptor<ElasticSearchConfiguration>
  {
    public static ListBoxModel doFillCredentialsIdItems(@QueryParameter String credentialsId)
    {
      StandardUsernameListBoxModel model = new StandardUsernameListBoxModel();

      model.includeEmptyValue()
          .includeAs(ACL.SYSTEM, (Item)null, StandardUsernamePasswordCredentials.class,
              Collections.<DomainRequirement> emptyList())
          .includeCurrentValue(credentialsId);

      return model;
    }

    public static ListBoxModel doFillCertificateIdItems(@QueryParameter String certificateId)
    {
      StandardListBoxModel model = new StandardListBoxModel();
      model.includeEmptyValue()
          .includeAs(ACL.SYSTEM, (Item)null, StandardCertificateCredentials.class)
          .includeCurrentValue(certificateId);

      return model;
    }

    public FormValidation doCheckKey(@QueryParameter("value") String value)
    {
      if (StringUtils.isBlank(value))
      {
        return FormValidation.warning("Key must not be empty");
      }
      return FormValidation.ok();
    }
  }

}
