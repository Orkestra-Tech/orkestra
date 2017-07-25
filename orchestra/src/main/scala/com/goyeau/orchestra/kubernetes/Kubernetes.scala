package com.goyeau.orchestra.kubernetes

import java.io.{FileInputStream, FileOutputStream}
import java.security.KeyStore
import java.security.cert.CertificateFactory

import scala.io.Source

import com.goyeau.orchestra.Config
import io.fabric8.kubernetes.client.{ConfigBuilder, DefaultKubernetesClient}

object Kubernetes {

  lazy val client = {
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
    keyStore.load(null, Array.empty)

    val certificateFactory = CertificateFactory.getInstance("X.509")
    val certificate = certificateFactory
      .generateCertificate(new FileInputStream("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"))
    keyStore.setCertificateEntry("kubernetes", certificate)

    val cacertsFile = "cacerts"
    val pass = "kubernetes"
    keyStore.store(new FileOutputStream(cacertsFile), pass.toCharArray)

    val config = new ConfigBuilder()
      .withMasterUrl(Config.kubeUri)
      .withOauthToken(Source.fromFile("/var/run/secrets/kubernetes.io/serviceaccount/token").mkString)
      .withTrustStoreFile(cacertsFile)
      .withTrustStorePassphrase(pass)
      .build
    new DefaultKubernetesClient(config)
  }
}
