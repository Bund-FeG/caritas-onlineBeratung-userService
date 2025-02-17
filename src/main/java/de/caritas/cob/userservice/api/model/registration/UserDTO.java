package de.caritas.cob.userservice.api.model.registration;

import static de.caritas.cob.userservice.api.helper.UserHelper.AGENCY_ID_MAX;
import static de.caritas.cob.userservice.api.helper.UserHelper.AGENCY_ID_MIN;
import static de.caritas.cob.userservice.api.helper.UserHelper.AGE_REGEXP;
import static de.caritas.cob.userservice.api.helper.UserHelper.CONSULTING_TYPE_REGEXP;
import static de.caritas.cob.userservice.api.helper.UserHelper.STATE_REGEXP;
import static de.caritas.cob.userservice.api.helper.UserHelper.TERMS_ACCEPTED_REGEXP;
import static de.caritas.cob.userservice.api.helper.UserHelper.VALID_POSTCODE_REGEX;

import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.caritas.cob.userservice.api.model.jsondeserializer.EncodeUsernameJsonDeserializer;
import de.caritas.cob.userservice.api.model.jsondeserializer.UrlDecodePasswordJsonDeserializer;
import de.caritas.cob.userservice.api.model.validation.ValidAge;
import de.caritas.cob.userservice.api.model.validation.ValidState;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User model
 *
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ApiModel(value = "User")
@ValidAge
@ValidState
@Builder
public class UserDTO implements UserRegistrationDTO {

  @NotBlank(message = "{user.username.notBlank}")
  @NotNull(message = "{user.username.notBlank}")
  @ApiModelProperty(required = true, example = "max94")
  @JsonDeserialize(using = EncodeUsernameJsonDeserializer.class)
  @JsonProperty("username")
  private String username;

  @NotBlank(message = "{user.custom.postcode.notNull}")
  @NotNull(message = "{user.custom.postcode.notNull}")
  @Pattern(regexp = VALID_POSTCODE_REGEX, message = "{user.custom.postcode.invalid}")
  @ApiModelProperty(required = true, example = "\"79098\"", position = 1)
  @JsonProperty("postcode")
  private String postcode;

  @NotNull(message = "{user.custom.agency.notNull}")
  @Min(value = AGENCY_ID_MIN, message = "{user.custom.agency.invalid}")
  @Max(value = AGENCY_ID_MAX, message = "{user.custom.agency.invalid}")
  @ApiModelProperty(required = true, example = "\"15\"", position = 2)
  @JsonProperty("agencyId")
  private Long agencyId;

  @NotBlank(message = "{user.password.notBlank}")
  @ApiModelProperty(required = true, example = "pass@w0rd", position = 3)
  @JsonDeserialize(using = UrlDecodePasswordJsonDeserializer.class)
  @JsonProperty("password")
  private String password;

  @JsonInclude(value = Include.NON_NULL)
  @Email(message = "{user.email.invalid}")
  @ApiModelProperty(example = "max@mustermann.de", position = 4)
  @JsonProperty("email")
  private String email;

  @JsonInclude(value = Include.NON_NULL)
  @Pattern(regexp = AGE_REGEXP, message = "{user.custom.age.invalid}")
  @ApiModelProperty(example = "1", position = 7)
  @JsonProperty("age")
  private String age;

  @JsonInclude(value = Include.NON_NULL)
  @Pattern(regexp = STATE_REGEXP, message = "{user.custom.state.invalid}")
  @JsonProperty("state")
  @ApiModelProperty(example = "\"16\"", position = 9)
  private String state;

  @Pattern(regexp = TERMS_ACCEPTED_REGEXP, message = "{user.custom.termsAccepted.invalid}")
  @ApiModelProperty(required = true, example = "\"true\"", position = 10)
  @JsonProperty("termsAccepted")
  private String termsAccepted;

  @Pattern(regexp = CONSULTING_TYPE_REGEXP, message = "{user.consultingType.invalid}")
  @ApiModelProperty(required = true, example = "\"0\"", position = 11)
  @JsonProperty("consultingType")
  private String consultingType;

  private boolean newUserAccount;

  public UserDTO(String email) {
    this.email = email;
  }

  public UserDTO(String username, String postcode, Long agencyId, String password, String email,
      String termsAccepted, String consultingType) {
    this.username = username;
    this.postcode = postcode;
    this.agencyId = agencyId;
    this.password = password;
    this.email = email;
    this.termsAccepted = termsAccepted;
    this.consultingType = consultingType;
  }

  public UserDTO(String age, String state, String consultingType) {
    this.age = age;
    this.state = state;
    this.consultingType = consultingType;
  }

  @Override
  public String toString() {
    return "UserDTO{"
        + "username='" + username + '\''
        + ", postcode='" + postcode + '\''
        + ", agencyId=" + agencyId
        + ", age='" + age + '\''
        + ", state='" + state + '\''
        + ", termsAccepted='" + termsAccepted + '\''
        + ", consultingType='" + consultingType + '\''
        + '}';
  }
}
