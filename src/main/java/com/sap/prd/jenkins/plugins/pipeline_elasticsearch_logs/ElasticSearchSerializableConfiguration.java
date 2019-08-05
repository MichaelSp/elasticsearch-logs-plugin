package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs.runid.RunIdProvider;

/**
 * A serializable representation of the plugin configuration with credentials resolved.
 * Reason: on remote side credentials cannot be accessed by credentialsId, same for keystore.
 *         That's why the values are transfered to remote.
 */
@Restricted(NoExternalUse.class)
public class ElasticSearchSerializableConfiguration implements Serializable
{
  private static final Logger LOGGER = Logger.getLogger(ElasticSearchSerializableConfiguration.class.getName());
  
  private static final long serialVersionUID = 1L;

  private final String username;

  private final String password;

  private final byte[] keyStoreBytes;

  private final String instanceId;

  private final RunIdProvider runIdProvider;

  private final URI uri;
  
  private transient KeyStore trustKeyStore;
  
  private final boolean saveAnnotations;

  public ElasticSearchSerializableConfiguration(URI uri, String username, String password,
        byte[] keyStoreBytes, String instanceId, RunIdProvider runIdProvider, boolean saveAnnotations)
  {
    super();
    this.uri = uri;
    this.username = username;
    this.password = password;
    this.instanceId = instanceId;
    this.runIdProvider = runIdProvider;
    if (keyStoreBytes != null)
    {
      this.keyStoreBytes = keyStoreBytes.clone();
    }
    else
    {
      this.keyStoreBytes = null;
    }
    this.saveAnnotations = saveAnnotations;
  }

  public boolean isSaveAnnotations()
  {
    return saveAnnotations;
  }

  public String getInstanceId()
  {
    return instanceId;
  }

  public RunIdProvider getRunIdProvider()
  {
    return runIdProvider;
  }

  public URI getUri()
  {
    return uri;
  }

  public String getUsername()
  {
    return username;
  }

  public String getPassword()
  {
    return password;
  }

  public KeyStore getTrustKeyStore()
  {
    if (trustKeyStore == null && keyStoreBytes != null)
    {
      try
      {
        trustKeyStore = KeyStore.getInstance("PKCS12");
        trustKeyStore.load(new ByteArrayInputStream(keyStoreBytes), "".toCharArray());
      }
      catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e)
      {
        LOGGER.log(Level.WARNING, "Failed to create KeyStore from bytes", e);
      }
    }
    return trustKeyStore;
  }

}
