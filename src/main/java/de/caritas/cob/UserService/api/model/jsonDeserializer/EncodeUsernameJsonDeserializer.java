package de.caritas.cob.UserService.api.model.jsonDeserializer;

import java.io.IOException;
import javax.ws.rs.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import de.caritas.cob.UserService.api.helper.UserHelper;

@Component
public class EncodeUsernameJsonDeserializer extends JsonDeserializer<String> {

  @Value("${user.username.invalid.length}")
  private String ERROR_USERNAME_INVALID_LENGTH;

  private UserHelper userHelper = new UserHelper();

  @Override
  public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException, JsonProcessingException {
    String username = userHelper.encodeUsername(jsonParser.getValueAsString());

    // Check if username is of valid length
    if (!userHelper.isUsernameValid(username)) {
      throw new BadRequestException(ERROR_USERNAME_INVALID_LENGTH);
    }

    return username;
  }

}
