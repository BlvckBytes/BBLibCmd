package me.blvckbytes.bblibcmd.exception;

import me.blvckbytes.bblibcmd.CommandHandlerSection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/23/2022

  The command sender has to be a player but in this case isn't.
*/
public class NotAPlayerException extends CommandException {

  public NotAPlayerException(CommandHandlerSection sect) {
    super(
      sect.getNotAPlayer()
        .withPrefix()
        .asComponent()
    );
  }
}
