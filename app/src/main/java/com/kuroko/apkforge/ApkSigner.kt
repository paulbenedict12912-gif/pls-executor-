package com.kuroko.apkforge

import android.util.Log
import com.android.apksig.ApkSigner
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Date

object ApkSigner {

    private const val TAG = "ApkForgeSign"
    private const val ALIAS = "apkforge"
    private const val PASSWORD = "apkforge"

    fun ensureKeystore(): File {
        val ksFile = File(ApkUtils.outputDir(), "apkforge.keystore")
        if (ksFile.exists()) return ksFile

        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()

        val now = System.currentTimeMillis()
        val name = X500Name("CN=ApkForge, O=Kuroko, C=PH")
        val certBuilder = JcaX509v3CertificateBuilder(
            name,
            BigInteger.valueOf(now),
            Date(now - 86_400_000L),
            Date(now + 10L * 365 * 86_400_000L),
            name,
            kp.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(kp.private)
        val cert: X509Certificate =
            JcaX509CertificateConverter().getCertificate(certBuilder.build(signer))

        val ks = KeyStore.getInstance("PKCS12")
        ks.load(null, null)
        ks.setKeyEntry(ALIAS, kp.private, PASSWORD.toCharArray(), arrayOf(cert))
        ksFile.outputStream().use { ks.store(it, PASSWORD.toCharArray()) }
        return ksFile
    }

    fun sign(inputApk: File, outputApk: File): Boolean {
        return try {
            val ksFile = ensureKeystore()
            val ks = KeyStore.getInstance("PKCS12")
            ksFile.inputStream().use { ks.load(it, PASSWORD.toCharArray()) }
            val key = ks.getKey(ALIAS, PASSWORD.toCharArray()) as java.security.PrivateKey
            val cert = ks.getCertificate(ALIAS) as X509Certificate

            val signerConfig = ApkSigner.SignerConfig.Builder(
                "CERT",
                key,
                listOf(cert)
            ).build()

            val apkSigner = ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(inputApk)
                .setOutputApk(outputApk)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .build()
            apkSigner.sign()
            true
        } catch (e: Exception) {
            Log.e(TAG, "sign failed", e)
            false
        }
    }
}
