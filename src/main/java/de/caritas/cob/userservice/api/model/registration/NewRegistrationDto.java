package de.caritas.cob.userservice.api.model.registration;

import static de.caritas.cob.userservice.api.helper.UserHelper.AGENCY_ID_MAX;
import static de.caritas.cob.userservice.api.helper.UserHelper.AGENCY_ID_MIN;
import static de.caritas.cob.userservice.api.helper.UserHelper.CONSULTING_TYPE_REGEXP;
import static de.caritas.cob.userservice.api.helper.UserHelper.POSTCODE_MAX;
import static de.caritas.cob.userservice.api.helper.UserHelper.POSTCODE_MIN;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.caritas.cob.userservice.api.model.validation.ValidPostcode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Model for new consulting type registrations
 *
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ApiModel(value = "NewRegistration")
@ValidPostcode
@ToString
public class NewRegistrationDto implements UserRegistrationDTO {

  @NotBlank(message = "{user.custom.postcode.notNull}")
  @Min(value = POSTCODE_MIN, message = "{user.custom.postcode.invalid}")
  @Max(value = POSTCODE_MAX, message = "{user.custom.postcode.invalid}")
  @ApiModelProperty(required = true, example = "\"79098\"", position = 0)
  @JsonProperty("postcode")
  private String postcode;

  @NotNull(message = "{user.custom.agency.notNull}")
  @Min(value = AGENCY_ID_MIN, message = "{user.custom.agency.invalid}")
  @Max(value = AGENCY_ID_MAX, message = "{user.custom.agency.invalid}")
  @ApiModelProperty(required = true, example = "\"15\"", position = 1)
  @JsonProperty("agencyId")
  private Long agencyId;

  @Pattern(regexp = CONSULTING_TYPE_REGEXP, message = "{user.consultingType.invalid}")
  @ApiModelProperty(required = true, example = "\"0\"", position = 2)
  @JsonProperty("consultingType")
  private String consultingType;

  @ApiModelProperty(hidden = true)
  private boolean newUserAccount;
}
