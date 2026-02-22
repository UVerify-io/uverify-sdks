package io.uverify.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.uverify.sdk.callback.DataSignature;
import io.uverify.sdk.exception.UVerifyException;
import io.uverify.sdk.exception.UVerifyValidationException;
import io.uverify.sdk.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UVerifyClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private HttpClient mockHttpClient;

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(int status, Object body) throws Exception {
        HttpResponse<String> resp = (HttpResponse<String>) mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(status);
        String json = body instanceof String ? (String) body : mapper.writeValueAsString(body);
        when(resp.body()).thenReturn(json);
        return resp;
    }

    private UVerifyClient client;

    @BeforeEach
    void setUp() {
        client = UVerifyClient.builder().httpClient(mockHttpClient).build();
    }

    // -------------------------------------------------------------------------
    // verify()
    // -------------------------------------------------------------------------

    @Test
    void verify_returnsCertificateList() throws Exception {
        List<Map<String, Object>> body = List.of(
                Map.of("hash", "abc", "transactionHash", "tx1", "creationTime", "2024-01-01")
        );
        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse(200, body));

        List<CertificateResponse> result = client.verify("abc");

        assertEquals(1, result.size());
        assertEquals("abc", result.get(0).getHash());
        assertEquals("tx1", result.get(0).getTransactionHash());
    }

    @Test
    void verify_returnsEmptyListOnNullBody() throws Exception {
        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse(200, ""));

        List<CertificateResponse> result = client.verify("abc");

        assertTrue(result.isEmpty());
    }

    @Test
    void verify_throwsUVerifyExceptionOn404() throws Exception {
        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse(404, "not found"));

        UVerifyException ex = assertThrows(UVerifyException.class, () -> client.verify("abc"));
        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void verify_throwsUVerifyExceptionOn500() throws Exception {
        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse(500, "server error"));

        UVerifyException ex = assertThrows(UVerifyException.class, () -> client.verify("abc"));
        assertEquals(500, ex.getStatusCode());
        assertEquals("server error", ex.getResponseBody());
    }

    // -------------------------------------------------------------------------
    // verifyByTransaction()
    // -------------------------------------------------------------------------

    @Test
    void verifyByTransaction_returnsCertificate() throws Exception {
        Map<String, Object> body = Map.of("hash", "h1", "transactionHash", "tx99");
        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse(200, body));

        CertificateResponse cert = client.verifyByTransaction("tx99", "h1");

        assertEquals("h1", cert.getHash());
        assertEquals("tx99", cert.getTransactionHash());
    }

    // -------------------------------------------------------------------------
    // UVerifyCore
    // -------------------------------------------------------------------------

    @Test
    void core_isNotNull() {
        assertNotNull(client.core);
    }

    @Test
    void core_buildTransaction_sendsPostAndReturnsResponse() throws Exception {
        Map<String, Object> body = Map.of("unsignedTransaction", "cbor-hex", "type", "default");
        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse(200, body));

        BuildTransactionResponse resp = client.core.buildTransaction(
                BuildTransactionRequest.defaultRequest(
                        "addr1...", "state-1", new CertificateData("hash1", "SHA-256")));

        assertEquals("cbor-hex", resp.getUnsignedTransaction());
    }

    @Test
    void core_submitTransaction_sendsPost() throws Exception {
        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse(200, ""));

        assertDoesNotThrow(() -> client.core.submitTransaction("signed-tx", "witness-set"));
        verify(mockHttpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void core_requestUserAction_returnsResponse() throws Exception {
        Map<String, Object> body = Map.of(
                "address", "addr1...", "action", "USER_INFO",
                "message", "challenge-msg", "signature", "server-sig",
                "timestamp", 1700000000L, "status", 200);
        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse(200, body));

        UserActionRequestResponse resp = client.core.requestUserAction(
                new UserActionRequest("addr1...", UserActionRequest.UserAction.USER_INFO));

        assertEquals("challenge-msg", resp.getMessage());
        assertEquals("server-sig", resp.getSignature());
    }

    @Test
    void core_executeUserAction_returnsResponse() throws Exception {
        Map<String, Object> body = Map.of("status", 200);
        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse(200, body));

        UserActionRequestResponse requestResp = new UserActionRequestResponse();
        requestResp.setAddress("addr1...");
        requestResp.setAction("USER_INFO");
        requestResp.setMessage("msg");
        requestResp.setSignature("sig");
        requestResp.setTimestamp(1700000000L);

        ExecuteUserActionResponse resp = client.core.executeUserAction(
                new ExecuteUserActionRequest(requestResp, "user-sig", "user-key"));

        assertEquals(200, resp.getStatus());
    }

    // -------------------------------------------------------------------------
    // issueCertificates()
    // -------------------------------------------------------------------------

    @Test
    void issueCertificates_buildsAndSubmitsTransaction() throws Exception {
        Map<String, Object> buildBody = Map.of("unsignedTransaction", "unsigned-cbor", "type", "bootstrap");
        when(mockHttpClient.send(any(), any()))
                .thenReturn(mockResponse(200, buildBody))
                .thenReturn(mockResponse(200, ""));

        client.issueCertificates(
                "addr1...",
                List.of(new CertificateData("hash1", "SHA-256")),
                unsignedTx -> "witness-set");

        verify(mockHttpClient, times(2)).send(any(), any());
    }

    @Test
    void issueCertificates_withStateId_usesDefaultType() throws Exception {
        Map<String, Object> buildBody = Map.of("unsignedTransaction", "unsigned-cbor", "type", "default");
        when(mockHttpClient.send(any(), any()))
                .thenReturn(mockResponse(200, buildBody))
                .thenReturn(mockResponse(200, ""));

        client.issueCertificates(
                "addr1...",
                "state-123",
                List.of(new CertificateData("hash1", "SHA-256")),
                unsignedTx -> "witness-set");

        verify(mockHttpClient, times(2)).send(any(), any());
    }

    @Test
    void issueCertificates_usesConstructorLevelCallback() throws Exception {
        UVerifyClient clientWithCallback = UVerifyClient.builder()
                .httpClient(mockHttpClient)
                .signTx(tx -> "default-witness")
                .build();

        Map<String, Object> buildBody = Map.of("unsignedTransaction", "unsigned-cbor", "type", "bootstrap");
        when(mockHttpClient.send(any(), any()))
                .thenReturn(mockResponse(200, buildBody))
                .thenReturn(mockResponse(200, ""));

        assertDoesNotThrow(() -> clientWithCallback.issueCertificates(
                "addr1...",
                List.of(new CertificateData("hash1", "SHA-256"))));
    }

    @Test
    void issueCertificates_throwsWhenNoCallback() {
        assertThrows(UVerifyValidationException.class, () ->
                client.issueCertificates("addr1...", List.of(new CertificateData("hash1"))));
    }

    // -------------------------------------------------------------------------
    // getUserInfo()
    // -------------------------------------------------------------------------

    @Test
    void getUserInfo_performsTwoStepFlow() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "address", "addr1...", "action", "USER_INFO",
                "message", "challenge", "signature", "server-sig",
                "timestamp", 1700000000L, "status", 200);
        Map<String, Object> executeBody = Map.of("status", 200);

        when(mockHttpClient.send(any(), any()))
                .thenReturn(mockResponse(200, requestBody))
                .thenReturn(mockResponse(200, executeBody));

        ExecuteUserActionResponse resp = client.getUserInfo(
                "addr1...",
                message -> new DataSignature("pub-key", "user-sig"));

        assertEquals(200, resp.getStatus());
        verify(mockHttpClient, times(2)).send(any(), any());
    }

    @Test
    void getUserInfo_usesConstructorLevelCallback() throws Exception {
        UVerifyClient clientWithCallback = UVerifyClient.builder()
                .httpClient(mockHttpClient)
                .signMessage(msg -> new DataSignature("key", "sig"))
                .build();

        Map<String, Object> requestBody = Map.of(
                "address", "addr1...", "action", "USER_INFO",
                "message", "challenge", "signature", "server-sig",
                "timestamp", 1700000000L, "status", 200);
        Map<String, Object> executeBody = Map.of("status", 200);

        when(mockHttpClient.send(any(), any()))
                .thenReturn(mockResponse(200, requestBody))
                .thenReturn(mockResponse(200, executeBody));

        assertDoesNotThrow(() -> clientWithCallback.getUserInfo("addr1..."));
    }

    @Test
    void getUserInfo_throwsWhenNoCallback() {
        assertThrows(UVerifyValidationException.class, () -> client.getUserInfo("addr1..."));
    }

    // -------------------------------------------------------------------------
    // invalidateState() / optOut()
    // -------------------------------------------------------------------------

    @Test
    void invalidateState_sendsStateId() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "address", "addr1...", "action", "INVALIDATE_STATE",
                "message", "challenge", "signature", "server-sig",
                "timestamp", 1700000000L, "status", 200);
        Map<String, Object> executeBody = Map.of("status", 200);

        when(mockHttpClient.send(any(), any()))
                .thenReturn(mockResponse(200, requestBody))
                .thenReturn(mockResponse(200, executeBody));

        ExecuteUserActionResponse resp = client.invalidateState(
                "addr1...", "state-123",
                message -> new DataSignature("key", "sig"));

        assertEquals(200, resp.getStatus());
    }

    @Test
    void optOut_sendsStateId() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "address", "addr1...", "action", "OPT_OUT",
                "message", "challenge", "signature", "server-sig",
                "timestamp", 1700000000L, "status", 200);
        Map<String, Object> executeBody = Map.of("status", 200);

        when(mockHttpClient.send(any(), any()))
                .thenReturn(mockResponse(200, requestBody))
                .thenReturn(mockResponse(200, executeBody));

        ExecuteUserActionResponse resp = client.optOut(
                "addr1...", "state-123",
                message -> new DataSignature("key", "sig"));

        assertEquals(200, resp.getStatus());
    }

    // -------------------------------------------------------------------------
    // Builder options
    // -------------------------------------------------------------------------

    @Test
    void builder_customBaseUrl() {
        UVerifyClient custom = UVerifyClient.builder()
                .baseUrl("https://custom.example.com")
                .httpClient(mockHttpClient)
                .build();
        assertNotNull(custom);
    }

    @Test
    void builder_customHeader() throws Exception {
        UVerifyClient custom = UVerifyClient.builder()
                .header("X-Api-Key", "secret")
                .httpClient(mockHttpClient)
                .build();

        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse(200, List.of()));

        custom.verify("hash");

        verify(mockHttpClient).send(
                argThat(req -> "secret".equals(req.headers().firstValue("X-Api-Key").orElse(""))),
                any());
    }
}
