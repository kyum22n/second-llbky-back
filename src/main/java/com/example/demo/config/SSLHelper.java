package com.example.demo.config;

import javax.net.ssl.*;

import org.springframework.context.annotation.Configuration;

import java.security.cert.X509Certificate;


/**
 * SSLHelper 클래스
 * ----------------
 * 이 클래스는 HTTPS 요청 시 SSL 인증서 검증을 **우회**하기 위한 설정을 제공합니다.
 * 주로 외부 API 호출 시, 자체 서명된 인증서(self-signed)나 신뢰되지 않은 인증서를
 * 사용할 때 발생하는 SSLHandshakeException을 방지하기 위해 사용합니다.
 * 
 * ⚠️ 주의: 실제 운영 환경에서는 보안상 위험하므로, 반드시 테스트/개발 환경에서만 사용해야 합니다.
 */

@Configuration // 스프링 컨텍스트에서 빈으로 등록 가능하도록 설정
public class SSLHelper {
    
    private static boolean initialized = false; 
    /** 
     * SSL 우회 설정이 이미 적용되었는지 확인하는 플래그
     * 여러 번 호출되더라도 한 번만 초기화되도록 함
     */
    
    public static void disableSSLVerification() {
        if (initialized) {
            return; // 이미 초기화되어 있으면 바로 종료
        }
        
        try {
            // -------------------------------
            // 1. TrustManager 배열 생성
            // -------------------------------
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    /** 클라이언트 인증서 검증 무시 */
                    public X509Certificate[] getAcceptedIssuers() {
                        return null; // null 반환 = 모든 인증서 허용
                    }
                    /** 클라이언트 인증서 검증 체크 무시 */
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // 아무 작업도 하지 않음 → 모든 클라이언트 인증서 허용
                    }
                    /** 서버 인증서 검증 체크 무시 */
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // 아무 작업도 하지 않음 → 모든 서버 인증서 허용
                    }
                }
            };

            // -------------------------------
            // 2. SSLContext 생성 및 초기화
            // -------------------------------
            SSLContext sc = SSLContext.getInstance("SSL"); 
            /** SSLContext 인스턴스 생성 ("SSL" 프로토콜 사용) */
            sc.init(null, trustAllCerts, new java.security.SecureRandom()); 
            /** 
             * 초기화:
             * - keyManager = null (클라이언트 인증서 사용 안함)
             * - trustManager = 모든 인증서 허용
             * - secureRandom = 난수 생성용
             */

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory()); 
            /** 모든 HttpsURLConnection 요청에 대해 위에서 생성한 SSLContext 사용 */

            // -------------------------------
            // 3. 호스트 이름 검증 무시
            // -------------------------------
            HostnameVerifier allHostsValid = (hostname, session) -> true; 
            /** 모든 호스트 이름을 신뢰하도록 설정 */
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid); 
            /** 모든 HttpsURLConnection 요청에 대해 호스트 이름 검증 비활성화 */

            initialized = true; 
            /** 초기화 완료 후 플래그 true 설정, 중복 호출 방지 */
            System.out.println("SSL 인증서 검증 우회 설정 완료"); 

        } catch (Exception e) {
            e.printStackTrace(); 
            /** SSL 초기화 중 예외 발생 시 스택 트레이스 출력 */
        }
    }
}
