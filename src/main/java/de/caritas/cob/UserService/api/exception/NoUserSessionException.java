package de.caritas.cob.UserService.api.exception;

public class NoUserSessionException extends RuntimeException {

  private static final long serialVersionUID = -5465524493469707522L;

  /**
   * Exception for enquiry message, when no user session is available
   * 
   * @param message
   */
  public NoUserSessionException(String message) {
    super(message);
  }


}
