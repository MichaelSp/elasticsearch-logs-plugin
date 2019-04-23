package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.annotation.Nonnull;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Restricted(NoExternalUse.class)
public class ElasticSearchSerializableConfiguration implements Serializable
{
  private static final long serialVersionUID = 1L;

  @Nonnull
  private final String host;

  private final int port;

  @Nonnull
  private final String key;

  private final String username;

  private final String password;

  private final boolean ssl;

  private final byte[] keyStoreBytes;

  public ElasticSearchSerializableConfiguration(String host, int port, String key, String username, String password,
      boolean ssl, byte[] keyStoreBytes)
  {
    super();
    this.host = host;
    this.port = port;
    this.key = key;
    this.username = username;
    this.password = password;
    this.ssl = ssl;
    if (keyStoreBytes != null)
    {
      this.keyStoreBytes = keyStoreBytes.clone();
    }
    else
    {
      this.keyStoreBytes = null;
    }
  }

  public String getHost()
  {
    return host;
  }

  public int getPort()
  {
    return port;
  }

  public String getKey()
  {
    return key;
  }

  public String getUsername()
  {
    return username;
  }

  public String getPassword()
  {
    return password;
  }

  public boolean isSsl()
  {
    return ssl;
  }

  public KeyStore getTrustKeyStore()
  {
    KeyStore keyStore = null;
    try
    {
      keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(new ByteArrayInputStream(keyStoreBytes), "".toCharArray());
    }
    catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return keyStore;
  }

}
