package com.goyeau.orchestra.kubernetes

import java.io.{FileInputStream, FileOutputStream}
import java.security.KeyStore
import java.security.cert.CertificateFactory

import scala.io.Source

import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}

object Auth {

  val token = {
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
    keyStore.load(null, Array.empty)

    val certificateFactory = CertificateFactory.getInstance("X.509")
    val cert = certificateFactory
      .generateCertificate(new FileInputStream("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"))
    keyStore.setCertificateEntry("kube", cert)

    keyStore.store(new FileOutputStream("cacerts"), Array.empty)
    System.setProperty("javax.net.ssl.trustStore", "cacerts")
    System.setProperty("javax.net.ssl.keyStore", "cacerts")

    Source.fromFile("/var/run/secrets/kubernetes.io/serviceaccount/token").mkString
  }

  val header = Authorization(OAuth2BearerToken(Auth.token))
}
