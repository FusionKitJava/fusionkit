package de.marcandreher.fusionkit.core.debug;

import org.jetbrains.annotations.NotNull;

import de.marcandreher.fusionkit.core.app.VersionInfo;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import lombok.Data;

public class FusionDebugRequestAPIHandler implements Handler {

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        DebugRequest debugRequest = new DebugRequest();
        debugRequest.setRequestId(ctx.attribute("debugKey"));
        debugRequest.setProtocol(ctx.req().getProtocol());
        debugRequest.setHost(ctx.req().getRemoteHost());
        debugRequest.setIp(ctx.req().getRemoteAddr());
        debugRequest.setUserAgent(ctx.req().getHeader("User-Agent"));
        debugRequest.setFusion(VersionInfo.getVersion());
        debugRequest.setBuild(VersionInfo.getBuildTimestamp());
        ctx.json(debugRequest);
    }

    @Data
    public static class DebugRequest {
        private String fusion;
        private String build;
        private String requestId;
        private String protocol;
        private String host;
        private String ip;
        private String userAgent;
    }
    
}
