package com.pomotodo.util;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

import java.security.*;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;

/**
 * VAPID 密钥生成工具类
 * 用于生成 Web Push 通知所需的 VAPID 密钥对
 */
public class VapidKeyGenerator {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {
        generateVapidKeys();
    }

    /**
     * 生成 VAPID 密钥对并打印
     */
    public static void generateVapidKeys() throws Exception {
        // 使用 P-256 曲线（secp256r1）
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyPairGenerator.initialize(spec);
        
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        
        // 获取公钥 - 转换为未压缩格式
        java.security.interfaces.ECPublicKey publicKey = 
            (java.security.interfaces.ECPublicKey) keyPair.getPublic();
        java.security.interfaces.ECPrivateKey privateKey = 
            (java.security.interfaces.ECPrivateKey) keyPair.getPrivate();
        
        // 获取公钥点坐标
        byte[] x = publicKey.getW().getAffineX().toByteArray();
        byte[] y = publicKey.getW().getAffineY().toByteArray();
        
        // 确保坐标是 32 字节（去除前导零）
        x = adjustTo32Bytes(x);
        y = adjustTo32Bytes(y);
        
        // 构建未压缩公钥: 0x04 + x + y (共 65 字节)
        byte[] uncompressedPublicKey = new byte[65];
        uncompressedPublicKey[0] = 0x04;
        System.arraycopy(x, 0, uncompressedPublicKey, 1, 32);
        System.arraycopy(y, 0, uncompressedPublicKey, 33, 32);
        
        // 获取私钥字节
        byte[] privateKeyBytes = privateKey.getS().toByteArray();
        privateKeyBytes = adjustTo32Bytes(privateKeyBytes);
        
        // 使用 URL-safe Base64 编码
        String publicKeyBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(uncompressedPublicKey);
        String privateKeyBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(privateKeyBytes);
        
        System.out.println("========== VAPID Keys Generated ==========");
        System.out.println("Public Key:");
        System.out.println(publicKeyBase64);
        System.out.println();
        System.out.println("Private Key:");
        System.out.println(privateKeyBase64);
        System.out.println();
        System.out.println("Subject (示例):");
        System.out.println("mailto:admin@pomotodo.com");
        System.out.println("==========================================");
        System.out.println();
        System.out.println("请将上述密钥配置到 application.yml 中:");
        System.out.println("vapid:");
        System.out.println("  public:");
        System.out.println("    key: ${VAPID_PUBLIC_KEY:" + publicKeyBase64 + "}");
        System.out.println("  private:");
        System.out.println("    key: ${VAPID_PRIVATE_KEY:" + privateKeyBase64 + "}");
        System.out.println("  subject: ${VAPID_SUBJECT:mailto:admin@pomotodo.com}");
    }
    
    /**
     * 调整字节数组为 32 字节长度
     */
    private static byte[] adjustTo32Bytes(byte[] input) {
        if (input.length == 32) {
            return input;
        }
        byte[] result = new byte[32];
        if (input.length > 32) {
            // 去除前导零
            System.arraycopy(input, input.length - 32, result, 0, 32);
        } else {
            // 前面补零
            System.arraycopy(input, 0, result, 32 - input.length, input.length);
        }
        return result;
    }
}
