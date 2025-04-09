package com.mixfa.tempmessages.model;

import lombok.NonNull;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public record FileResponse(
        @NonNull
        String filename,
        @NonNull
        StreamingResponseBody streamingResponse
) {
}
