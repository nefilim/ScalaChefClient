package org.nefilim.chefclient

import java.security.{Signature, MessageDigest, KeyFactory, PrivateKey}
import org.apache.commons.codec.binary.Base64
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Created by peter on 4/4/14.
 */
object ChefClientCryptUtil {

  def getPrivateKey(filename: String): PrivateKey = {
    val source = scala.io.Source.fromFile(filename)
    val sourceLines = source.getLines.toBuffer
    val pemString = sourceLines.drop(1).dropRight(1).mkString // drop BEGIN RSA PRIVATE KEY and END RSA PRIVATE KEY
    source.close()
    val encoded: Array[Byte] = Base64.decodeBase64(pemString)

    // PKCS8 decode the encoded RSA private key
    val keySpec = new PKCS8EncodedKeySpec(encoded)
    val kf = KeyFactory.getInstance("RSA")
    kf.generatePrivate(keySpec)
  }

  def sha1(input: String): Array[Byte] = {
    val hash: MessageDigest = MessageDigest.getInstance("SHA-1")
    hash.update(input.getBytes("UTF-8"))
    return hash.digest
  }

  def sha1(input: Array[Byte]): Array[Byte] = {
    val hash: MessageDigest = MessageDigest.getInstance("SHA-1")
    hash.update(input)
    return hash.digest
  }

  def signData(data: Array[Byte], key: PrivateKey): Array[Byte] = {
    val signer: Signature = Signature.getInstance("NoneWithRSA")
    signer.initSign(key)
    signer.update(data)
    return (signer.sign)
  }

}