package com.minh.fakebook.user.config;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import java.io.IOException;
import org.springframework.data.domain.PageRequest;

public class CachedPageRequestSerializer extends JsonSerializer<PageRequest> {
    private void fields(PageRequest value, JsonGenerator generator) throws IOException {
        generator.writeNumberField("pageNumber", value.getPageNumber());
        generator.writeNumberField("pageSize", value.getPageSize());
        generator.writeArrayFieldStart("sortOrders");
        for (var order : value.getSort()) {
            generator.writeStartObject();
            generator.writeStringField("property", order.getProperty());
            generator.writeStringField("direction", order.getDirection().name());
            generator.writeStringField("nullHandling", order.getNullHandling().name());
            generator.writeBooleanField("ignoreCase", order.isIgnoreCase());
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }
    @Override public void serialize(PageRequest value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        fields(value, generator);
        generator.writeEndObject();
    }
    @Override public void serializeWithType(PageRequest value, JsonGenerator generator, SerializerProvider provider,
        TypeSerializer serializer) throws IOException {
        var type = serializer.writeTypePrefix(generator, serializer.typeId(value, JsonToken.START_OBJECT));
        fields(value, generator);
        serializer.writeTypeSuffix(generator, type);
    }
}
