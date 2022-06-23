/*
 * Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.tracing.telemetry;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * This class used to generate log telemetry tracer related logs.
 */
public class LogExporter implements SpanExporter {

    private static final Logger logger = Logger.getLogger(LogExporter.class.getName());
    private final Log log;
    private final JsonFactory jsonFactory = new JsonFactory();

    public LogExporter() {

        this.log = LogFactory.getLog(TelemetryConstants.TRACER);
    }

    public static LogExporter create() {

        return new LogExporter();
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {

        Iterator var3 = spans.iterator();
        StringWriter writer;
        JsonGenerator generator;
        while (var3.hasNext()) {
            try {
                writer = new StringWriter();
                generator = this.jsonFactory.createGenerator(writer);
                generator.writeStartObject();
                SpanData span = (SpanData) var3.next();
                generator.writeStringField(TelemetryConstants.SPAN_ID, span.getSpanId());
                generator.writeStringField(TelemetryConstants.TRACER_ID, span.getTraceId());
                generator.writeStringField(TelemetryConstants.OPERATION_NAME, span.getName());
                generator.writeStringField(TelemetryConstants.LATENCY,
                        ((int) (span.getEndEpochNanos() - span.getStartEpochNanos()) / 1000000) + "ms");
                generator.writeStringField(TelemetryConstants.ATTRIBUTES, String.valueOf(span.getAttributes()));
                generator.writeEndObject();
                generator.close();
                writer.close();
                log.trace(writer.toString());
            } catch (IOException e) {
                log.error("Error in structured message when exporting", e);
            }
        }

        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {

        CompletableResultCode resultCode = new CompletableResultCode();
        Handler[] var2 = logger.getHandlers();
        int var3 = var2.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            Handler handler = var2[var4];

            try {
                handler.flush();
            } catch (Throwable var7) {
                resultCode.fail();
            }
        }

        return resultCode.succeed();
    }

    public CompletableResultCode shutdown() {

        return this.flush();
    }
}
