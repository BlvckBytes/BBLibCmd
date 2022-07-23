package me.blvckbytes.bblibcmd.exception;

import me.blvckbytes.bblibcmd.CommandHandlerSection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/23/2022

  The specified duration is malformed.
*/
public class InvalidDurationException extends CommandException {

  public InvalidDurationException(CommandHandlerSection sect, String input) {
    super(
      sect.getInvalidDuration()
        .withPrefix()
        .withVariable("input", input)
        .asComponent()
    );
  }
}
