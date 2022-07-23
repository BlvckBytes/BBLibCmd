package me.blvckbytes.bblibcmd.exception;

import me.blvckbytes.bblibcmd.CommandHandlerSection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/23/2022

  The specified UUID is malformed.
*/
public class InvalidUuidException extends CommandException {

  public InvalidUuidException(CommandHandlerSection sect, String input) {
    super(
      sect.getInvalidUuid()
        .withPrefix()
        .withVariable("input", input)
        .asComponent()
    );
  }
}
