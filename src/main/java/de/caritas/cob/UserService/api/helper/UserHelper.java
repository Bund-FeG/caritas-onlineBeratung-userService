package de.caritas.cob.UserService.api.helper;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.representations.idm.UserRepresentation;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import de.caritas.cob.UserService.api.exception.HelperException;
import de.caritas.cob.UserService.api.repository.chat.Chat;
import de.caritas.cob.UserService.api.repository.session.ConsultingType;
import de.caritas.cob.UserService.api.repository.user.User;
import de.caritas.cob.UserService.api.service.UserService;
import de.caritas.cob.UserService.api.service.helper.KeycloakAdminClientHelper;

@Component
public class UserHelper {

  @Value("${keycloakService.user.dummySuffix}")
  private String EMAIL_DUMMY_SUFFIX;

  @Value("${app.base.url}")
  private String HOST_BASE_URL;

  public static final int USERNAME_MIN_LENGTH = 5;
  public static final int USERNAME_MAX_LENGTH = 30;
  public static final long POSTCODE_MIN = 0;
  public static final long POSTCODE_MAX = 99999;
  public static final long AGENCY_ID_MIN = 0;
  public static final long AGENCY_ID_MAX = Long.MAX_VALUE;
  public static final String ADDICTIVE_DRUGS_REGEXP = "(([0-9]+,)+[0-9]+)|[0-9]+|";
  public static final String RELATION_REGEXP = "[0-9]+|";
  public static final String AGE_REGEXP = "[0-9]+|";
  public static final String GENDER_REGEXP = "[0-9]+|";
  public static final String STATE_REGEXP = "[0-9]|1[0-6]";
  public static final String TERMS_ACCEPTED_REGEXP = "true|TRUE";
  public static final String CONSULTING_TYPE_REGEXP = "[0-9]+|";
  public static final String CHAT_DURATION_REGEXP = "[0-9]+";
  public static final String CHAT_AGENCIES_REGEXP = "(([0-9]+,)+[0-9]+)|[0-9]+|";
  public static final int CHAT_MIN_DURATION = 30;
  public static final int CHAT_MAX_DURATION = 180;
  public static final int CHAT_TOPIC_MIN_LENGTH = 3;
  public static final int CHAT_TOPIC_MAX_LENGTH = 50;
  private final String ENCODING_PREFIX = "enc.";
  private final String BASE32_PLACEHOLDER = "=";
  private final String BASE32_PLACEHOLDER_USERNAME_REPLACE_STRING = ".";
  private final String BASE32_PLACEHOLDER_CHAT_ID_REPLACE_STRING = "";
  private Base32 base32 = new Base32();

  @Autowired
  private KeycloakAdminClientHelper keycloakAdminClientHelper;
  @Autowired
  private UserService userService;

  /**
   * Generates a random password which complies with the Keycloak policy
   * 
   * @return
   */
  public String getRandomPassword() {
    List<CharacterRule> rules = Arrays.asList(
        // at least one upper-case character
        new CharacterRule(EnglishCharacterData.UpperCase, 1),
        // at least one lower-case character
        new CharacterRule(EnglishCharacterData.LowerCase, 1),
        // at least one digit character
        new CharacterRule(EnglishCharacterData.Digit, 1),
        // at least one special character
        new CharacterRule(new CharacterData() {
          @Override
          public String getErrorCode() {
            return "ERR_SPECIAL";
          }

          @Override
          public String getCharacters() {
            return "!()$%&";
          }
        }, 1));
    PasswordGenerator generator = new PasswordGenerator();
    // Generated password is 8 characters long, which complies with policy
    return generator.generatePassword(10, rules);
  }

  /**
   * Generates the dummy email for a Keycloak user
   * 
   * @param userId
   * @return
   */
  public String getDummyEmail(String userId) {
    return userId + EMAIL_DUMMY_SUFFIX;
  }

  /**
   * Checks if the given username is between minimum and maximum char length
   * 
   * @param username
   * @return
   */
  public boolean isUsernameValid(String username) {
    username = decodeUsername(username);
    return username.length() >= USERNAME_MIN_LENGTH && username.length() <= USERNAME_MAX_LENGTH;
  }

  /**
   * Returns the Base32 encoded username. The padding char "=" of the Base32 String will be replaced
   * by a dot "." to support Rocket.Chat special chars.
   * 
   * @param username
   * @return encoded username
   */
  private String base32EncodeUsername(String username) {
    return ENCODING_PREFIX + base32EncodeAndReplacePlaceholder(username, BASE32_PLACEHOLDER,
        BASE32_PLACEHOLDER_USERNAME_REPLACE_STRING);
  }

  /**
   * Returns the Base32 decoded username. Placeholder dot "." (to support Rocket.Chat special chars)
   * will be replaced by the Base32 padding symbol "=".
   * 
   * @param username
   * @return
   */
  private String base32DecodeUsername(String username) {
    try {
      return new String(base32.decode(username.replace(ENCODING_PREFIX, StringUtils.EMPTY)
          .toUpperCase().replace(BASE32_PLACEHOLDER_USERNAME_REPLACE_STRING, BASE32_PLACEHOLDER)));

    } catch (Exception exception) {
      // Catch generic exception because of lack of base32 documentation
      throw new HelperException(String.format("Could not decode username %s", username), exception);
    }
  }

  /**
   * Encodes the given username if it isn't already encoded
   * 
   * @param username
   * @return encoded username
   */
  public String encodeUsername(String username) {
    return username.startsWith(ENCODING_PREFIX) ? username : base32EncodeUsername(username);
  }

  /**
   * Descodes the given username if it isn't already decoded
   * 
   * @param username
   * @return
   */
  public String decodeUsername(String username) {
    return username.startsWith(ENCODING_PREFIX) ? base32DecodeUsername(username) : username;
  }

  /**
   * Returns true if the decoded username does not exist in Keycloak yet or false if it already
   * exists.
   * 
   * @param username (decoded or encoded)
   * @return true if does not exist, else false
   */
  public boolean isUsernameAvailable(String username) {
    List<UserRepresentation> keycloakUserList =
        keycloakAdminClientHelper.findByUsername(decodeUsername(username));
    for (UserRepresentation userRep : keycloakUserList) {
      if (userRep.getUsername().equalsIgnoreCase(decodeUsername(username))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns true if the given usernames match.
   * 
   * @param firstUsername encoded or decoded first username to compare
   * @param secondUsername encoded or decoded second username to compare
   * @return
   */
  public boolean doUsernamesMatch(String firstUsername, String secondUsername) {
    return StringUtils.equals(encodeUsername(firstUsername).toLowerCase(),
        encodeUsername(secondUsername).toLowerCase());
  }

  /**
   * Base32 encodes a given String
   * 
   * @param value String to be encoded
   * @return encoded String
   */
  private String base32EncodeAndReplacePlaceholder(String value, String placeholder,
      String replaceString) {
    try {
      return base32.encodeAsString(value.getBytes()).toString().replace(placeholder, replaceString);

    } catch (Exception exception) {
      // Catch generic exception because of lack of base32 documentation
      throw new HelperException(String.format("Could not encode value %s", value), exception);
    }
  }

  /**
   * Generates the URL for a chat with the given {@link Chat} id and {@link ConsultingType}
   * 
   * @param chatId the {@link Chat}'s id
   * @param consultingType the chat's {@link ConsultingType}
   * @return URL (String)
   */
  public String generateChatUrl(Long chatId, ConsultingType consultingType) {
    return HOST_BASE_URL + "/" + consultingType.getUrlName() + "/"
        + base32EncodeAndReplacePlaceholder(Long.toString(chatId), BASE32_PLACEHOLDER,
            BASE32_PLACEHOLDER_CHAT_ID_REPLACE_STRING);
  }

  /**
   * Updates/sets the user's Rocket.Chat ID in MariaDB if not already set.
   * 
   * @param user {@link User}
   * @param rcUserId Rocket.Chat user ID
   */
  public void updateRocketChatIdInDatabase(User user, String rcUserId) {
    if (user != null && StringUtils.isEmpty(user.getRcUserId())) {
      user.setRcUserId(rcUserId);
      userService.saveUser(user);
    }
  }

}
