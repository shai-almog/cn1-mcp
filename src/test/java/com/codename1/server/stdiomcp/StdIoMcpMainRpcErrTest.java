package com.codename1.server.stdiomcp;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StdIoMcpMainRpcErrTest {

    @Test
    void rpcErrExposesErrorPayload() {
        Map<String, Object> errorPayload = Map.of("code", -32000, "message", "Internal error");

        StdIoMcpMain.RpcErr rpcErr = new StdIoMcpMain.RpcErr("2.0", 99, errorPayload);

        assertEquals("2.0", rpcErr.jsonrpc());
        assertEquals(99, rpcErr.id());
        assertSame(errorPayload, rpcErr.error());
    }
}
