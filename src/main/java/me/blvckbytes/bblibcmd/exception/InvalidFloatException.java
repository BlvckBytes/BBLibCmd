package me.blvckbytes.bblibcmd.exception;

import me.blvckbytes.bblibcmd.CommandHandlerSection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/23/2022

  The specified float is malformed.
*/
public class InvalidFloatException extends CommandException {

  public InvalidFloatException(CommandHandlerSection sect, String input) {
    super(
      sect.getInvalidDuration()
        .withPrefix()
        .withVariable("input", input)
        .asComponent()
    );
  }
}
