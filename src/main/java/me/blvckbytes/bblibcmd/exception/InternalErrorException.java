package me.blvckbytes.bblibcmd.exception;

import me.blvckbytes.bblibcmd.CommandHandlerSection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/23/2022

  An internal error occurred.
*/
public class InternalErrorException extends CommandException {

  public InternalErrorException(CommandHandlerSection sect) {
    super(
      sect.getInternalError()
        .withPrefix()
        .asComponent()
    );
  }
}
