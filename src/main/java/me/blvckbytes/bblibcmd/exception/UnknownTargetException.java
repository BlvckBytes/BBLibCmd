package me.blvckbytes.bblibcmd.exception;

import me.blvckbytes.bblibcmd.CommandHandlerSection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/23/2022

  The target player has never played on this server before.
*/
public class UnknownTargetException extends CommandException {

  public UnknownTargetException(CommandHandlerSection sect, String name) {
    super(
      sect.getUnknownTarget()
        .withPrefix()
        .withVariable("name", name)
        .asComponent()
    );
  }
}
