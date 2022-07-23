package me.blvckbytes.bblibcmd.exception;

import me.blvckbytes.bblibcmd.CommandHandlerSection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/23/2022

  The target player is offline but required to be online for the process to work.
*/
public class OfflineTargetException extends CommandException {

  public OfflineTargetException(CommandHandlerSection sect, String name) {
    super(
      sect.getOfflineTarget()
        .withPrefix()
        .withVariable("name", name)
        .asComponent()
    );
  }
}
