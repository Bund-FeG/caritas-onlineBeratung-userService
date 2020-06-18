package de.caritas.cob.UserService.api.helper;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Special json serializer for empty objects. Necessary for some Rocket.Chat API calls.
 */
public class EmptyObjectSerializer extends StdSerializer<Object> {

  private static final long serialVersionUID = 1L;

  public EmptyObjectSerializer() {
    super(Object.class);
  }

  protected EmptyObjectSerializer(Class<Object> t) {
    super(t);
  }

  @Override
  public void serialize(Object value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeString("{}");
  }

}
