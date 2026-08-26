package com.company.supportticketing.exception;
import java.util.UUID;
public class UserNotFoundException extends RuntimeException { public UserNotFoundException(UUID id) { super("User " + id + " was not found"); } }
