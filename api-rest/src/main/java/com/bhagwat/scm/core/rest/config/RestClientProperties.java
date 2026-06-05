package com.bhagwat.scm.core.rest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

import static com.bhagwat.scm.core.rest.config.Constants.*;

@ConfigurationProperties(prefix = "service.core.rest-client")
@Data
public class RestClientProperties {
    private int maxBufferSize = 10485760;
    private int requestTimeOut = 5000;
    private int responseTimeOut = 5000;
    private int connectionTimeOut = 5000;
    private Tyk tyk = new Tyk();
    private CorrelationId correlationId = new CorrelationId();
    private Logging logging = new Logging();

    public void setMaxBufferSize(int maxBufferSize){
        if(maxBufferSize <= 0){
            throw new IllegalArgumentException();
        }
        this.maxBufferSize = maxBufferSize;
    }
    @Data
    public static class CorrelationId{
        private String coreMdcKey = CORRELATION_MDC_KEY;
        private String outgoingHeader = CORRELATION_OUTGOING_HEADER;
    }

    @Data
    public static class Logging{
        private boolean enabled = false;
        private RequestBody requestBody = new RequestBody();
        private ResponseBody responseBody = new ResponseBody();
        private List<String> includePaths = new ArrayList<>();
        private List<String> excludePaths = new ArrayList<>();

        @Data
        public static class RequestBody{
            private boolean enabled = false;
            private int maxSize = 1024;
        }

        @Data
        public static class ResponseBody{
            private boolean enabled = false;
            private int maxSize = 1024;
        }
    }

    @Data
    public static class Tyk{
        private boolean headerKeyEnabled = false;
        private String headerKey = TYK_HEADER_KEY;
        private String headerValue;
    }

}
