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

@Restricted(NoExternalUse.class)
public class ElasticSearchSerializableConfiguration implements Serializable
{
  private static final Logger LOGGER = Logger.getLogger(ElasticSearchSerializableConfiguration.class.getName());
  
  private static final long serialVersionUID = 1L;

  private final String username;

  private final String password;

  private final byte[] keyStoreBytes;

  private final String instanceId;
  
  private final URI uri;
  
  private transient KeyStore trustKeyStore;

  public ElasticSearchSerializableConfiguration(URI uri, String username, String password,
        byte[] keyStoreBytes, String instanceId)
  {
    super();
    this.uri = uri;
    this.username = username;
    this.password = password;
    this.instanceId = instanceId;
    if (keyStoreBytes != null)
    {
      this.keyStoreBytes = keyStoreBytes.clone();
    }
    else
    {
      this.keyStoreBytes = null;
    }
  }

  public String getInstanceId()
  {
    return instanceId;
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
